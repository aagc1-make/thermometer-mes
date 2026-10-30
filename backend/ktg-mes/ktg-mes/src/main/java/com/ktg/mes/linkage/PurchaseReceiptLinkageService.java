package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.utils.DateUtils;
import com.ktg.mes.qc.domain.QcIqc;
import com.ktg.mes.qc.service.IQcIqcService;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmBatch;
import com.ktg.mes.wm.domain.WmItemRecpt;
import com.ktg.mes.wm.domain.WmItemRecptLine;
import com.ktg.mes.wm.service.IWmBatchService;
import com.ktg.mes.wm.service.IWmItemRecptLineService;
import com.ktg.mes.wm.service.IWmItemRecptService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * IQC 全部完成后，为同一到货通知单幂等生成一张采购入库草稿。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>同一到货通知只生成一张草稿（数据库 linkage_key 唯一约束兜底，并发时返回已存在的草稿）；</li>
 *   <li>只有合格量 &gt; 0 的来源行才写入草稿明细，NG 行不入库；</li>
 *   <li>全 NG 时不生成没有可入库行的空草稿；</li>
 *   <li>草稿状态为 PREPARE，仍需仓管员分配库位/批次并执行，保存不等于过账。</li>
 * </ul>
 */
@Service
public class PurchaseReceiptLinkageService {

    private final IWmItemRecptService receiptService;
    private final IWmItemRecptLineService receiptLineService;
    private final IWmBatchService batchService;
    private final AutoCodeUtil autoCodeUtil;
    private final MenuLinkageNoticeService menuNoticeService;
    private final IQcIqcService iqcService;

    public PurchaseReceiptLinkageService(IWmItemRecptService receiptService,
                                         IWmItemRecptLineService receiptLineService,
                                         IWmBatchService batchService,
                                         AutoCodeUtil autoCodeUtil,
                                         MenuLinkageNoticeService menuNoticeService,
                                         IQcIqcService iqcService) {
        this.receiptService = receiptService;
        this.receiptLineService = receiptLineService;
        this.batchService = batchService;
        this.autoCodeUtil = autoCodeUtil;
        this.menuNoticeService = menuNoticeService;
        this.iqcService = iqcService;
    }

    @Transactional
    public WmItemRecpt createDraftWhenReady(WmArrivalNotice notice,
                                            List<WmArrivalNoticeLine> sourceLines,
                                            String operator) {
        if (notice == null || sourceLines == null || sourceLines.isEmpty()) {
            return null;
        }
        // 兼容历史数据：早期完成 IQC 时因状态判断缺陷没有回写到货行（iqc_id 为空），
        // 这里以 qc_iqc 表为准补一次判断——只要该来源行存在"已完成且合格量>0"的检验单，
        // 就视为已检，不再让整单一直卡在"等待检验"。
        for (WmArrivalNoticeLine line : sourceLines) {
            if (!UserConstants.YES.equals(line.getIqcCheck())) {
                continue;
            }
            if (line.getIqcId() == null) {
                QcIqc completed = findCompletedIqc(line.getLineId());
                if (completed == null) {
                    return null;
                }
                line.setIqcId(completed.getIqcId());
                line.setIqcCode(completed.getIqcCode());
                if (line.getQuantityQuanlified() == null) {
                    line.setQuantityQuanlified(completed.getQuantityQualified());
                }
            }
        }

        WmItemRecpt existing = findExisting(notice.getNoticeId());
        if (existing != null) {
            return existing;
        }

        // 先确定可入库明细：只有合格量 > 0 的行才进入草稿，全 NG 则整单不建。
        List<WmItemRecptLine> draftLines = new ArrayList<>();
        for (WmArrivalNoticeLine sourceLine : sourceLines) {
            BigDecimal quantity = UserConstants.YES.equals(sourceLine.getIqcCheck())
                    ? sourceLine.getQuantityQuanlified() : sourceLine.getQuantityArrival();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            draftLines.add(buildReceiptLine(notice, sourceLine, quantity, operator));
        }
        if (draftLines.isEmpty()) {
            return null;
        }

        WmItemRecpt receipt = new WmItemRecpt();
        receipt.setRecptCode(autoCodeUtil.genSerialCode(UserConstants.ITEMRECPT_CODE, ""));
        receipt.setRecptName("到货通知" + notice.getNoticeCode() + "采购入库");
        receipt.setNoticeId(notice.getNoticeId());
        receipt.setNoticeCode(notice.getNoticeCode());
        receipt.setPoCode(notice.getPoCode());
        receipt.setVendorId(notice.getVendorId());
        receipt.setVendorCode(notice.getVendorCode());
        receipt.setVendorName(notice.getVendorName());
        receipt.setVendorNick(notice.getVendorNick());
        receipt.setRecptDate(DateUtils.getNowDate());
        receipt.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        receipt.setAttr1("AUTO:ITEM_RECPT:ARRIVAL_NOTICE:" + notice.getNoticeId());
        receipt.setCreateBy(operator);
        try {
            receiptService.insertWmItemRecpt(receipt);
        } catch (DuplicateKeyException concurrent) {
            // 并发提交：唯一约束保证只落一张，返回已存在的那张。
            WmItemRecpt winner = findExisting(notice.getNoticeId());
            if (winner != null) {
                return winner;
            }
            throw concurrent;
        }

        for (WmItemRecptLine line : draftLines) {
            line.setRecptId(receipt.getRecptId());
            receiptLineService.insertWmItemRecptLine(line);
        }
        try {
            menuNoticeService.markUnread(MenuLinkageNoticePaths.ITEM_RECPT);
        } catch (Exception ignore) {
            // 提醒失败不影响已生成的入库草稿
        }
        return receipt;
    }

    private WmItemRecpt findExisting(Long noticeId) {
        WmItemRecpt query = new WmItemRecpt();
        query.setNoticeId(noticeId);
        List<WmItemRecpt> existing = receiptService.selectWmItemRecptList(query);
        if (existing == null || existing.isEmpty()) {
            return null;
        }
        return existing.get(0);
    }

    /**
     * 按来源到货行查已完成且有合格量的来料检验单（用于兼容未回写的历史数据）。
     */
    private QcIqc findCompletedIqc(Long arrivalLineId) {
        if (arrivalLineId == null || iqcService == null) {
            return null;
        }
        QcIqc query = new QcIqc();
        query.setSourceDocType(UserConstants.SOURCE_DOC_TYPE_ARRIVALNOTICE);
        query.setSourceLineId(arrivalLineId);
        List<QcIqc> list = iqcService.selectQcIqcList(query);
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (QcIqc iqc : list) {
            boolean completed = UserConstants.ORDER_STATUS_CONFIRMED.equals(iqc.getStatus())
                    || UserConstants.ORDER_STATUS_FINISHED.equals(iqc.getStatus());
            if (completed && iqc.getQuantityQualified() != null
                    && iqc.getQuantityQualified().compareTo(BigDecimal.ZERO) > 0) {
                return iqc;
            }
        }
        return null;
    }

    private WmItemRecptLine buildReceiptLine(WmArrivalNotice notice,
                                             WmArrivalNoticeLine source,
                                             BigDecimal quantity,
                                             String operator) {
        WmItemRecptLine line = new WmItemRecptLine();
        line.setNoticeLineId(source.getLineId());
        line.setItemId(source.getItemId());
        line.setItemCode(source.getItemCode());
        line.setItemName(source.getItemName());
        line.setSpecification(source.getSpecification());
        line.setUnitOfMeasure(source.getUnitOfMeasure());
        line.setUnitName(source.getUnitName());
        line.setQuantityRecived(quantity);
        line.setIqcCheck(source.getIqcCheck());
        line.setIqcId(source.getIqcId());
        line.setIqcCode(source.getIqcCode());
        // 明细级幂等键：同一来源行只能生成一条入库明细。
        line.setAttr1("AUTO:ITEM_RECPT_LINE:ARRIVAL_LINE:" + source.getLineId());
        line.setCreateBy(operator);

        WmBatch batchQuery = new WmBatch();
        batchQuery.setItemId(source.getItemId());
        batchQuery.setItemCode(source.getItemCode());
        batchQuery.setItemName(source.getItemName());
        batchQuery.setSpecification(source.getSpecification());
        batchQuery.setUnitOfMeasure(source.getUnitOfMeasure());
        batchQuery.setUnitName(source.getUnitName());
        batchQuery.setPoCode(notice.getPoCode());
        batchQuery.setVendorId(notice.getVendorId());
        batchQuery.setVendorCode(notice.getVendorCode());
        batchQuery.setVendorName(notice.getVendorName());
        batchQuery.setVendorNick(notice.getVendorNick());
        WmBatch batch = batchService.getOrGenerateBatchCode(batchQuery);
        if (batch != null) {
            line.setBatchId(batch.getBatchId());
            line.setBatchCode(batch.getBatchCode());
        }
        return line;
    }
}
