package com.ktg.mes.wm.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.VendorReturnGuardMapper;
import com.ktg.mes.wm.service.*;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** 从已执行的采购入库批次生成可调整的退供草稿。 */
@Service
public class StockVendorReturnDraftService {
    private final PostingLockMapper locks; private final VendorReturnGuardMapper guards;
    private final IWmRtVendorService vendors; private final IWmRtVendorLineService vendorLines;
    private final IWmItemRecptService receipts; private final IWmItemRecptLineService receiptLines;
    private final IWmItemRecptDetailService details; private final AutoCodeUtil codes;
    public StockVendorReturnDraftService(PostingLockMapper locks, VendorReturnGuardMapper guards,
            IWmRtVendorService vendors, IWmRtVendorLineService vendorLines, IWmItemRecptService receipts,
            IWmItemRecptLineService receiptLines, IWmItemRecptDetailService details, AutoCodeUtil codes) {
        this.locks=locks;this.guards=guards;this.vendors=vendors;this.vendorLines=vendorLines;
        this.receipts=receipts;this.receiptLines=receiptLines;this.details=details;this.codes=codes;
    }
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long recptId,String operator) {
        if (recptId==null || !UserConstants.ORDER_STATUS_FINISHED.equals(locks.itemRecpt(recptId)))
            throw new ServiceException("只有已执行的采购入库单可生成退供草稿");
        Long existing=guards.openStockDraft(recptId);
        if (existing!=null) return existing;
        WmItemRecpt receipt=receipts.selectWmItemRecptByRecptId(recptId);
        if (receipt==null) throw new ServiceException("来源采购入库单不存在");
        WmItemRecptLine filter=new WmItemRecptLine();filter.setRecptId(recptId);
        List<WmItemRecptLine> source=receiptLines.selectWmItemRecptLineList(filter);
        if (source==null || source.isEmpty()) throw new ServiceException("来源采购入库单没有明细行");
        List<WmRtVendorLine> draftLines=new ArrayList<WmRtVendorLine>();
        for (WmItemRecptLine line:source) {
            WmItemRecptDetail df=new WmItemRecptDetail();df.setRecptId(recptId);df.setLineId(line.getLineId());
            List<WmItemRecptDetail> allocations=details.selectWmItemRecptDetailList(df);
            if (allocations==null || allocations.isEmpty()) throw new ServiceException("来源入库行缺少批次上架明细");
            Map<String,WmItemRecptDetail> batches=new LinkedHashMap<String,WmItemRecptDetail>();
            Map<String,BigDecimal> quantities=new LinkedHashMap<String,BigDecimal>();
            for (WmItemRecptDetail d:allocations) {
                if (!Objects.equals(recptId,d.getRecptId()) || !Objects.equals(line.getLineId(),d.getLineId())
                        || !Objects.equals(line.getItemId(),d.getItemId()) || d.getQuantity()==null || d.getQuantity().signum()<=0)
                    throw new ServiceException("来源入库明细的物料或数量不正确");
                String key=String.valueOf(d.getBatchId())+":"+String.valueOf(d.getBatchCode());
                batches.put(key,d);quantities.put(key,quantities.containsKey(key)?quantities.get(key).add(d.getQuantity()):d.getQuantity());
            }
            for (String key:batches.keySet()) {
                WmItemRecptDetail d=batches.get(key);
                BigDecimal returned=guards.finishedBatchReturned(line.getLineId(),d.getBatchId(),d.getBatchCode(),-1L);
                if (returned==null) returned=BigDecimal.ZERO;
                BigDecimal available=quantities.get(key).subtract(returned);
                if (available.signum()<=0) continue;
                WmRtVendorLine item=new WmRtVendorLine();
                item.setItemId(line.getItemId());item.setItemCode(line.getItemCode());item.setItemName(line.getItemName());
                item.setSpecification(line.getSpecification());item.setUnitOfMeasure(line.getUnitOfMeasure());item.setUnitName(line.getUnitName());
                item.setBatchId(d.getBatchId());item.setBatchCode(d.getBatchCode());item.setQuantityRted(available);
                item.setAttr2(String.valueOf(line.getLineId()));item.setCreateBy(operator);draftLines.add(item);
            }
        }
        if (draftLines.isEmpty()) throw new ServiceException("来源采购入库批次没有可退数量");
        WmRtVendor header=new WmRtVendor();header.setRtCode(codes.genSerialCode(UserConstants.WM_RTVENDOR_CODE,null));
        header.setRtName("采购入库退供-"+receipt.getRecptCode());header.setPoCode(receipt.getPoCode());
        header.setVendorId(receipt.getVendorId());header.setVendorCode(receipt.getVendorCode());
        header.setVendorName(receipt.getVendorName());header.setVendorNick(receipt.getVendorNick());
        header.setAttr1("SRC:RECEIPT:"+recptId);header.setAttr2("STOCK");header.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        header.setRtDate(new Date());header.setCreateBy(operator);
        if (vendors.insertWmRtVendor(header)!=1 || header.getRtId()==null) throw new ServiceException("创建退供单失败");
        for (WmRtVendorLine item:draftLines) {
            item.setRtId(header.getRtId());
            if (vendorLines.insertWmRtVendorLine(item)!=1) throw new ServiceException("创建退供行失败");
        }
        return header.getRtId();
    }
}
