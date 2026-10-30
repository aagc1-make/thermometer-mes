package com.ktg.mes.wm.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.qc.domain.QcIqc;
import com.ktg.mes.qc.service.IQcIqcService;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.VendorReturnGuardMapper;
import com.ktg.mes.wm.mapper.WmMaterialStockMapper;
import com.ktg.mes.wm.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/** 采购退供共同过账入口：未入库模式不产生库存事务，已入库模式核对来源与库存。 */
@Service
public class VendorReturnWorkflowService {
    private final PostingLockMapper locks;
    private final VendorReturnGuardMapper guards;
    private final IWmRtVendorService headers;
    private final IWmRtVendorLineService lines;
    private final IWmRtVendorDetailService details;
    private final IQcIqcService iqcs;
    private final IWmItemRecptLineService receiptLines;
    private final WmMaterialStockMapper stocks;
    private final IStorageCoreService storage;
    public VendorReturnWorkflowService(PostingLockMapper locks, VendorReturnGuardMapper guards,
            IWmRtVendorService headers, IWmRtVendorLineService lines, IWmRtVendorDetailService details,
            IQcIqcService iqcs, IWmItemRecptLineService receiptLines, WmMaterialStockMapper stocks,
            IStorageCoreService storage) {
        this.locks=locks; this.guards=guards; this.headers=headers; this.lines=lines; this.details=details;
        this.iqcs=iqcs; this.receiptLines=receiptLines; this.stocks=stocks; this.storage=storage;
    }

    /** 调用方必须处于事务内，父单锁保持到编辑提交。 */
    public void prepareEdit(WmRtVendor request) {
        Long id=request.getRtId();String previous=id==null?null:locks.rtVendor(id);
        if (previous==null || UserConstants.ORDER_STATUS_FINISHED.equals(previous)
                || UserConstants.ORDER_STATUS_CANCELED.equals(previous)
                || UserConstants.ORDER_STATUS_FINISHED.equals(request.getStatus()))
            throw new ServiceException("退供单不存在、已完成或已取消，完成状态只能通过执行产生");
        WmRtVendor db=headers.selectWmRtVendorByRtId(id);
        if (db==null) throw new ServiceException("退供单不存在");
        if ((request.getAttr1()!=null && !Objects.equals(request.getAttr1(),db.getAttr1()))
                || (request.getAttr2()!=null && !Objects.equals(request.getAttr2(),db.getAttr2()))
                || (("PRE_STOCK".equals(db.getAttr2()) || "STOCK".equals(db.getAttr2()))
                    && request.getVendorId()!=null && !Objects.equals(request.getVendorId(),db.getVendorId())))
            throw new ServiceException("退供来源或供应商不能修改");
        request.setAttr1(db.getAttr1());request.setAttr2(db.getAttr2());
        if ("PRE_STOCK".equals(db.getAttr2()) || "STOCK".equals(db.getAttr2())) {
            request.setVendorId(db.getVendorId());request.setVendorCode(db.getVendorCode());
            request.setVendorName(db.getVendorName());request.setVendorNick(db.getVendorNick());request.setPoCode(db.getPoCode());
        }
        if (UserConstants.ORDER_STATUS_PREPARE.equals(request.getStatus())
                && !UserConstants.ORDER_STATUS_PREPARE.equals(previous)
                && !(("PRE_STOCK".equals(db.getAttr2()) || "STOCK".equals(db.getAttr2()))
                     && (UserConstants.ORDER_STATUS_APPROVING.equals(previous) || UserConstants.ORDER_STATUS_APPROVED.equals(previous))))
            throw new ServiceException("已提交退供单不能退回草稿");
        if (UserConstants.ORDER_STATUS_APPROVING.equals(request.getStatus())
                && !UserConstants.ORDER_STATUS_PREPARE.equals(previous)
                && !UserConstants.ORDER_STATUS_APPROVING.equals(previous)
                && !("STOCK".equals(db.getAttr2()) && UserConstants.ORDER_STATUS_APPROVED.equals(previous)))
            throw new ServiceException("退供单当前状态不能重新提交拣货");
        if ("PRE_STOCK".equals(db.getAttr2()) && UserConstants.ORDER_STATUS_PREPARE.equals(previous)
                && UserConstants.ORDER_STATUS_APPROVING.equals(request.getStatus()))
            request.setStatus(UserConstants.ORDER_STATUS_APPROVED);
        if (UserConstants.ORDER_STATUS_APPROVED.equals(request.getStatus())
                && !UserConstants.ORDER_STATUS_APPROVING.equals(previous)
                && !UserConstants.ORDER_STATUS_APPROVED.equals(previous)
                && !("PRE_STOCK".equals(db.getAttr2()) && UserConstants.ORDER_STATUS_PREPARE.equals(previous)))
            throw new ServiceException("退供单尚未完成拣货或来源校验");
        if ("PRE_STOCK".equals(db.getAttr2()) && UserConstants.ORDER_STATUS_APPROVED.equals(request.getStatus())) {
            Long iqcId=parseKey(db.getAttr1(),"AUTO:RTV:IQC:");
            if (!"FINISHED".equals(locks.iqc(iqcId))) throw new ServiceException("来源来料检验尚未完成");
            WmRtVendorLine filter=new WmRtVendorLine();filter.setRtId(id);
            List<WmRtVendorLine> rows=lines.selectWmRtVendorLineList(filter);
            if (rows==null || rows.isEmpty()) throw new ServiceException("未入库退供缺少来源行");
            validatePreStock(id,iqcId,rows);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(Long id) {
        if (id == null || !UserConstants.ORDER_STATUS_APPROVED.equals(locks.rtVendor(id)))
            throw new ServiceException("退供单不存在、已执行或不在待执行状态");
        String mode=guards.lockMode(id), key=guards.lockSourceKey(id);
        Long source;
        if ("PRE_STOCK".equals(mode)) {
            source=parseKey(key,"AUTO:RTV:IQC:");
            if (!"FINISHED".equals(locks.iqc(source))) throw new ServiceException("来源来料检验尚未完成");
        } else if ("STOCK".equals(mode)) {
            source=parseKey(key,"SRC:RECEIPT:");
            if (!UserConstants.ORDER_STATUS_FINISHED.equals(locks.itemRecpt(source)))
                throw new ServiceException("来源采购入库单尚未完成");
            locks.inventory();
        } else {
            throw new ServiceException("退供单缺少可信来源，请从检验或采购入库生成");
        }
        WmRtVendor header=headers.selectWmRtVendorByRtId(id);
        if (header==null || !Objects.equals(mode,header.getAttr2()) || !Objects.equals(key,header.getAttr1()))
            throw new ServiceException("退供来源已发生变化，请刷新重试");
        WmRtVendorLine query=new WmRtVendorLine();query.setRtId(id);
        List<WmRtVendorLine> rows=lines.selectWmRtVendorLineList(query);
        if (rows==null || rows.isEmpty()) throw new ServiceException("请添加退供物料");
        if ("PRE_STOCK".equals(mode)) validatePreStock(id,source,rows);
        else validateStock(id,source,rows);
        if ("STOCK".equals(mode)) storage.processRtVendor(headers.getTxBeans(id));
        header.setStatus(UserConstants.ORDER_STATUS_FINISHED);
        if (headers.updateWmRtVendor(header)!=1) throw new ServiceException("退供单状态更新失败");
    }

    private void validatePreStock(Long id,Long iqcId,List<WmRtVendorLine> rows) {
        QcIqc iqc=iqcs.selectQcIqcByIqcId(iqcId);
        if (iqc==null || !UserConstants.SOURCE_DOC_TYPE_ARRIVALNOTICE.equals(iqc.getSourceDocType())
                || iqc.getSourceLineId()==null || iqc.getQuantityUnqualified()==null
                || iqc.getQuantityUnqualified().signum()<=0)
            throw new ServiceException("未入库退供缺少有效的不合格检验来源");
        BigDecimal total=BigDecimal.ZERO;
        for (WmRtVendorLine row:rows) {
            if (!Objects.equals(id,row.getRtId()) || !Objects.equals(iqc.getItemId(),row.getItemId())
                    || !Objects.equals("IQC:"+iqcId,row.getAttr1())
                    || !Objects.equals(String.valueOf(iqc.getSourceLineId()),row.getAttr2()))
                throw new ServiceException("未入库退供数量或来源与不合格检验结果不一致");
            if (row.getQuantityRted()==null || row.getQuantityRted().signum()<=0)
                throw new ServiceException("未入库退供数量必须大于零");
            total=total.add(row.getQuantityRted());
        }
        // 累计守恒：本次 + 该 IQC 其他已执行退供 <= 不合格数量；未执行的其它草稿不占用可退量
        BigDecimal finished=guards.finishedPreStockReturned(iqcId,id);
        if (finished==null) finished=BigDecimal.ZERO;
        BigDecimal remaining=iqc.getQuantityUnqualified().subtract(finished);
        if (total.compareTo(remaining)>0)
            throw new ServiceException("未入库退供累计数量 " + plain(total) + " 超过剩余可退数量 " + plain(remaining)
                    + "（不合格 " + plain(iqc.getQuantityUnqualified()) + "，已执行退供 " + plain(finished) + "）");
        WmRtVendorDetail filter=new WmRtVendorDetail();filter.setRtId(id);
        List<WmRtVendorDetail> stockDetails=details.selectWmRtVendorDetailList(filter);
        if (stockDetails!=null && !stockDetails.isEmpty())
            throw new ServiceException("未入库退供不能关联库存拣货明细");
    }

    private void validateStock(Long id,Long recptId,List<WmRtVendorLine> rows) {
        Map<String,BigDecimal> currentByBatch=new HashMap<String,BigDecimal>();
        Map<String,WmRtVendorLine> reference=new HashMap<String,WmRtVendorLine>();
        Map<Long,BigDecimal> byStock=new TreeMap<Long,BigDecimal>();
        Map<Long,WmRtVendorDetail> detailByStock=new HashMap<Long,WmRtVendorDetail>();
        for (WmRtVendorLine row:rows) {
            Long sourceLine=parseId(row.getAttr2());
            WmItemRecptLine receipt=receiptLines.selectWmItemRecptLineByLineId(sourceLine);
            if (receipt==null || !Objects.equals(recptId,receipt.getRecptId())
                    || !Objects.equals(receipt.getItemId(),row.getItemId()) || !Objects.equals(id,row.getRtId()))
                throw new ServiceException("退供行与来源采购入库行不一致");
            if (row.getQuantityRted()==null || row.getQuantityRted().signum()<=0)
                throw new ServiceException("退供数量必须大于零");
            String batchKey=sourceLine+":"+String.valueOf(row.getBatchId())+":"+String.valueOf(row.getBatchCode());
            currentByBatch.put(batchKey,currentByBatch.containsKey(batchKey)?currentByBatch.get(batchKey).add(row.getQuantityRted()):row.getQuantityRted());
            reference.put(batchKey,row);
            WmRtVendorDetail filter=new WmRtVendorDetail();filter.setRtId(id);filter.setLineId(row.getLineId());
            List<WmRtVendorDetail> allocations=details.selectWmRtVendorDetailList(filter);
            if (allocations==null || allocations.isEmpty()) throw new ServiceException("退供行尚未拣货");
            BigDecimal sum=BigDecimal.ZERO;
            for (WmRtVendorDetail d:allocations) {
                if (!Objects.equals(id,d.getRtId()) || !Objects.equals(row.getLineId(),d.getLineId())
                        || !Objects.equals(row.getItemId(),d.getItemId())
                        || !Objects.equals(row.getBatchId(),d.getBatchId()) || !Objects.equals(row.getBatchCode(),d.getBatchCode())
                        || d.getMaterialStockId()==null || d.getQuantity()==null || d.getQuantity().signum()<=0)
                    throw new ServiceException("退供拣货的来源、物料、批次或数量不正确");
                sum=sum.add(d.getQuantity()); Long stockId=d.getMaterialStockId();
                byStock.put(stockId,byStock.containsKey(stockId)?byStock.get(stockId).add(d.getQuantity()):d.getQuantity());
                WmRtVendorDetail prior=detailByStock.putIfAbsent(stockId,d);
                if (prior!=null && (!Objects.equals(prior.getItemId(),d.getItemId()) || !Objects.equals(prior.getBatchId(),d.getBatchId())
                        || !Objects.equals(prior.getBatchCode(),d.getBatchCode())
                        || !Objects.equals(prior.getWarehouseId(),d.getWarehouseId()) || !Objects.equals(prior.getLocationId(),d.getLocationId())
                        || !Objects.equals(prior.getAreaId(),d.getAreaId()) || !Objects.equals(prior.getWarehouseCode(),d.getWarehouseCode())
                        || !Objects.equals(prior.getLocationCode(),d.getLocationCode()) || !Objects.equals(prior.getAreaCode(),d.getAreaCode())))
                    throw new ServiceException("同一库存行的退供拣货信息不一致");
            }
            if (sum.compareTo(row.getQuantityRted())!=0) throw new ServiceException("退供拣货合计必须等于退供数量");
        }
        for (Map.Entry<String,BigDecimal> entry:currentByBatch.entrySet()) {
            WmRtVendorLine row=reference.get(entry.getKey());Long sourceLine=parseId(row.getAttr2());
            BigDecimal received=guards.receivedBatchQuantity(recptId,sourceLine,row.getBatchId(),row.getBatchCode());
            BigDecimal returned=guards.finishedBatchReturned(sourceLine,row.getBatchId(),row.getBatchCode(),id);
            if (received==null || returned==null || returned.add(entry.getValue()).compareTo(received)>0)
                throw new ServiceException("累计退供数量超过来源采购入库批次的数量");
        }
        for (Map.Entry<Long,BigDecimal> entry:byStock.entrySet()) {
            WmMaterialStock stock=stocks.lockMaterialStockById(entry.getKey());WmRtVendorDetail d=detailByStock.get(entry.getKey());
            if (stock==null || !Objects.equals(stock.getItemId(),d.getItemId()) || !Objects.equals(stock.getBatchId(),d.getBatchId())
                    || !Objects.equals(stock.getBatchCode(),d.getBatchCode()) || !Objects.equals(stock.getWarehouseId(),d.getWarehouseId())
                    || !Objects.equals(stock.getLocationId(),d.getLocationId()) || !Objects.equals(stock.getAreaId(),d.getAreaId())
                    || !Objects.equals(stock.getWarehouseCode(),d.getWarehouseCode()) || !Objects.equals(stock.getLocationCode(),d.getLocationCode())
                    || !Objects.equals(stock.getAreaCode(),d.getAreaCode()))
                throw new ServiceException("退供拣货与库存行不一致");
            if (stock.getQuantityOnhand()==null || stock.getQuantityOnhand().compareTo(entry.getValue())<0)
                throw new ServiceException("退供库存不足");
        }
    }
    private Long parseKey(String key,String prefix) {
        if (key==null || !key.startsWith(prefix)) throw new ServiceException("退供单缺少可信来源");
        return parseId(key.substring(prefix.length()));
    }
    private String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }
    private Long parseId(String text) {
        try { Long n=Long.valueOf(text); if(n<=0)throw new NumberFormatException(); return n; }
        catch (RuntimeException e) { throw new ServiceException("退供来源编号无效"); }
    }
}
