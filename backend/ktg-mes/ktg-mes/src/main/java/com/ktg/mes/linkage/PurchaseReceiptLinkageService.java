package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.utils.DateUtils;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmBatch;
import com.ktg.mes.wm.domain.WmItemRecpt;
import com.ktg.mes.wm.domain.WmItemRecptLine;
import com.ktg.mes.wm.service.IWmBatchService;
import com.ktg.mes.wm.service.IWmItemRecptLineService;
import com.ktg.mes.wm.service.IWmItemRecptService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** IQC 全部完成后，为同一到货通知单幂等生成一张采购入库草稿。 */
@Service
public class PurchaseReceiptLinkageService {

    private final IWmItemRecptService receiptService;
    private final IWmItemRecptLineService receiptLineService;
    private final IWmBatchService batchService;
    private final AutoCodeUtil autoCodeUtil;
    private final MenuLinkageNoticeService menuNoticeService;

    public PurchaseReceiptLinkageService(IWmItemRecptService receiptService,
                                         IWmItemRecptLineService receiptLineService,
                                         IWmBatchService batchService,
                                         AutoCodeUtil autoCodeUtil,
                                         MenuLinkageNoticeService menuNoticeService) {
        this.receiptService = receiptService;
        this.receiptLineService = receiptLineService;
        this.batchService = batchService;
        this.autoCodeUtil = autoCodeUtil;
        this.menuNoticeService = menuNoticeService;
    }

    @Transactional
    public WmItemRecpt createDraftWhenReady(WmArrivalNotice notice,
                                            List<WmArrivalNoticeLine> sourceLines,
                                            String operator) {
        if (notice == null || sourceLines == null || sourceLines.isEmpty()) {
            return null;
        }
        for (WmArrivalNoticeLine line : sourceLines) {
            if (UserConstants.YES.equals(line.getIqcCheck()) && line.getIqcId() == null) {
                return null;
            }
        }

        WmItemRecpt query = new WmItemRecpt();
        query.setNoticeId(notice.getNoticeId());
        List<WmItemRecpt> existing = receiptService.selectWmItemRecptList(query);
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
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
        receiptService.insertWmItemRecpt(receipt);

        for (WmArrivalNoticeLine sourceLine : sourceLines) {
            BigDecimal quantity = UserConstants.YES.equals(sourceLine.getIqcCheck())
                    ? sourceLine.getQuantityQuanlified() : sourceLine.getQuantityArrival();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            WmItemRecptLine line = buildReceiptLine(receipt, notice, sourceLine, quantity, operator);
            receiptLineService.insertWmItemRecptLine(line);
        }
        menuNoticeService.markUnread(MenuLinkageNoticePaths.ITEM_RECPT);
        return receipt;
    }

    private WmItemRecptLine buildReceiptLine(WmItemRecpt receipt,
                                             WmArrivalNotice notice,
                                             WmArrivalNoticeLine source,
                                             BigDecimal quantity,
                                             String operator) {
        WmItemRecptLine line = new WmItemRecptLine();
        line.setRecptId(receipt.getRecptId());
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
