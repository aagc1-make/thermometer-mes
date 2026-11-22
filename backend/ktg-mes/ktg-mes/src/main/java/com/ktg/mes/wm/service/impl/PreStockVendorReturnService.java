package com.ktg.mes.wm.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.qc.domain.QcIqc;
import com.ktg.mes.qc.service.IQcIqcService;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmRtVendor;
import com.ktg.mes.wm.domain.WmRtVendorLine;
import com.ktg.mes.wm.mapper.VendorReturnGuardMapper;
import com.ktg.mes.wm.service.IWmArrivalNoticeLineService;
import com.ktg.mes.wm.service.IWmArrivalNoticeService;
import com.ktg.mes.wm.service.IWmRtVendorLineService;
import com.ktg.mes.wm.service.IWmRtVendorService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * IQC 未入库不合格数量对应的退供草稿。
 *
 * 数量口径：
 * - 可退量 = 该 IQC 不合格数量 - 该 IQC 已执行(FINISHED)退供累计量；
 * - 未执行草稿只占“进行中”名额（同一 IQC 至多一个进行中草稿），不占用可退量；
 * - 草稿取消、退回或删除后，进行中名额释放，可重新生成。
 * 调用方须持有来源 IQC 行锁（保证同一 IQC 的创建/执行串行）。
 */
@Service
public class PreStockVendorReturnService {
    private final IWmRtVendorService headers;
    private final IWmRtVendorLineService lines;
    private final VendorReturnGuardMapper guards;
    private final AutoCodeUtil codes;
    private final IQcIqcService iqcs;
    private final IWmArrivalNoticeService notices;
    private final IWmArrivalNoticeLineService noticeLines;

    public PreStockVendorReturnService(IWmRtVendorService headers, IWmRtVendorLineService lines,
                                       VendorReturnGuardMapper guards, AutoCodeUtil codes) {
        this(headers, lines, guards, codes, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PreStockVendorReturnService(IWmRtVendorService headers, IWmRtVendorLineService lines,
                                       VendorReturnGuardMapper guards, AutoCodeUtil codes,
                                       IQcIqcService iqcs, IWmArrivalNoticeService notices,
                                       IWmArrivalNoticeLineService noticeLines) {
        this.headers = headers;
        this.lines = lines;
        this.guards = guards;
        this.codes = codes;
        this.iqcs = iqcs;
        this.notices = notices;
        this.noticeLines = noticeLines;
    }

    /** 按 IQC 重新生成（或复用）未入库退供草稿，供“取消/删除后重新生成”使用。 */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromIqc(Long iqcId, String operator) {
        if (iqcs == null || notices == null || noticeLines == null) {
            throw new ServiceException("未配置来源查询服务");
        }
        if (iqcId == null) {
            throw new ServiceException("缺少来料检验单");
        }
        QcIqc iqc = iqcs.selectQcIqcByIqcId(iqcId);
        if (iqc == null || !UserConstants.SOURCE_DOC_TYPE_ARRIVALNOTICE.equals(iqc.getSourceDocType())
                || iqc.getSourceDocId() == null || iqc.getSourceLineId() == null) {
            throw new ServiceException("该检验单不是到货通知来源，无法生成未入库退供");
        }
        WmArrivalNotice notice = notices.selectWmArrivalNoticeByNoticeId(iqc.getSourceDocId());
        WmArrivalNoticeLine line = noticeLines.selectWmArrivalNoticeLineByLineId(iqc.getSourceLineId());
        if (notice == null || line == null) {
            throw new ServiceException("来源到货通知或到货行不存在");
        }
        if (remaining(iqc).signum() <= 0) {
            return null;
        }
        // 传入 IQC 的不合格总量，由 create 统一按“已执行累计”计算剩余可退
        return create(iqc, notice, line, iqc.getQuantityUnqualified(), operator);
    }

    /** 该 IQC 当前剩余可退量（不合格数量 - 已执行累计）。 */
    public BigDecimal remaining(QcIqc iqc) {
        if (iqc == null || iqc.getQuantityUnqualified() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal finished = guards.finishedPreStockReturned(iqc.getIqcId(), -1L);
        if (finished == null) {
            finished = BigDecimal.ZERO;
        }
        return iqc.getQuantityUnqualified().subtract(finished);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(QcIqc iqc, WmArrivalNotice notice, WmArrivalNoticeLine line, BigDecimal quantity, String operator) {
        if (iqc == null || iqc.getIqcId() == null || notice == null || line == null
                || !Objects.equals(iqc.getSourceDocId(), notice.getNoticeId())
                || !Objects.equals(iqc.getSourceLineId(), line.getLineId())
                || !Objects.equals(notice.getNoticeId(), line.getNoticeId())
                || !Objects.equals(iqc.getItemId(), line.getItemId())) {
            throw new ServiceException("不合格退供来源不完整或不一致");
        }
        // 1) 同一 IQC 已有进行中草稿：直接复用，不新建、不改数量
        Long open = guards.openPreStockDraft(iqc.getIqcId());
        if (open != null) {
            return open;
        }
        // 2) 计算剩余可退量：本次不合格总量(quantity) 或已持久化的不合格量 - 已执行累计
        BigDecimal base = (quantity != null && quantity.signum() > 0) ? quantity
                : (iqc.getQuantityUnqualified() == null ? BigDecimal.ZERO : iqc.getQuantityUnqualified());
        BigDecimal finished = guards.finishedPreStockReturned(iqc.getIqcId(), -1L);
        if (finished == null) {
            finished = BigDecimal.ZERO;
        }
        BigDecimal remaining = base.subtract(finished);
        if (remaining.signum() <= 0) {
            return null;
        }
        BigDecimal requested = remaining;
        // 3) 创建草稿（只占进行中名额，不占已执行累计）
        WmRtVendor header = new WmRtVendor();
        header.setRtCode(codes.genSerialCode(UserConstants.WM_RTVENDOR_CODE, null));
        header.setRtName("来料不合格退供-" + iqc.getIqcCode());
        header.setVendorId(notice.getVendorId());
        header.setVendorCode(notice.getVendorCode());
        header.setVendorName(notice.getVendorName());
        header.setVendorNick(notice.getVendorNick());
        header.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        header.setAttr1("AUTO:RTV:IQC:" + iqc.getIqcId());
        header.setAttr2("PRE_STOCK");
        header.setRtReason("未入库不合格品退供");
        header.setRtDate(new java.util.Date());
        header.setCreateBy(operator);
        if (headers.insertWmRtVendor(header) != 1 || header.getRtId() == null) {
            throw new ServiceException("创建未入库退供草稿失败");
        }
        WmRtVendorLine item = new WmRtVendorLine();
        item.setRtId(header.getRtId());
        item.setItemId(line.getItemId());
        item.setItemCode(line.getItemCode());
        item.setItemName(line.getItemName());
        item.setSpecification(line.getSpecification());
        item.setUnitOfMeasure(line.getUnitOfMeasure());
        item.setUnitName(line.getUnitName());
        item.setQuantityRted(requested);
        item.setAttr1("IQC:" + iqc.getIqcId());
        item.setAttr2(String.valueOf(line.getLineId()));
        item.setCreateBy(operator);
        if (lines.insertWmRtVendorLine(item) != 1) {
            throw new ServiceException("创建未入库退供行失败");
        }
        return header.getRtId();
    }
}
