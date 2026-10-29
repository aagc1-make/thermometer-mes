package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmBatch;
import com.ktg.mes.wm.domain.WmItemRecpt;
import com.ktg.mes.wm.domain.WmItemRecptLine;
import com.ktg.mes.wm.service.IWmBatchService;
import com.ktg.mes.wm.service.IWmItemRecptLineService;
import com.ktg.mes.wm.service.IWmItemRecptService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseReceiptLinkageServiceTest {

    private IWmItemRecptService receiptService;
    private IWmItemRecptLineService receiptLineService;
    private IWmBatchService batchService;
    private AutoCodeUtil autoCodeUtil;
    private MenuLinkageNoticeService menuNoticeService;
    private PurchaseReceiptLinkageService linkageService;

    @BeforeEach
    void setUp() {
        receiptService = mock(IWmItemRecptService.class);
        receiptLineService = mock(IWmItemRecptLineService.class);
        batchService = mock(IWmBatchService.class);
        autoCodeUtil = mock(AutoCodeUtil.class);
        menuNoticeService = mock(MenuLinkageNoticeService.class);
        linkageService = new PurchaseReceiptLinkageService(
                receiptService, receiptLineService, batchService, autoCodeUtil, menuNoticeService,
                com.ktg.mes.qc.service.IQcIqcService.class.cast(
                        mock(com.ktg.mes.qc.service.IQcIqcService.class)));
    }

    @Test
    void createsOneReceiptDraftWithQualifiedArrivalQuantity() {
        WmArrivalNotice notice = notice();
        WmArrivalNoticeLine line = qualifiedLine();
        WmBatch batch = new WmBatch();
        batch.setBatchId(55L);
        batch.setBatchCode("B-001");
        when(receiptService.selectWmItemRecptList(any(WmItemRecpt.class)))
                .thenReturn(Collections.emptyList());
        when(autoCodeUtil.genSerialCode(UserConstants.ITEMRECPT_CODE, ""))
                .thenReturn("REC-001");
        when(receiptService.insertWmItemRecpt(any(WmItemRecpt.class))).thenAnswer(invocation -> {
            invocation.<WmItemRecpt>getArgument(0).setRecptId(100L);
            return 1;
        });
        when(batchService.getOrGenerateBatchCode(any(WmBatch.class))).thenReturn(batch);

        linkageService.createDraftWhenReady(notice, Collections.singletonList(line), "tester");

        ArgumentCaptor<WmItemRecptLine> captor = ArgumentCaptor.forClass(WmItemRecptLine.class);
        verify(receiptLineService).insertWmItemRecptLine(captor.capture());
        assertEquals(line.getLineId(), captor.getValue().getNoticeLineId());
        assertEquals(new BigDecimal("8"), captor.getValue().getQuantityRecived());
        assertEquals(55L, captor.getValue().getBatchId());
        verify(menuNoticeService).markUnread(MenuLinkageNoticePaths.ITEM_RECPT);
    }

    @Test
    void doesNotCreateReceiptUntilEveryRequiredLineHasFinishedIqc() {
        WmArrivalNoticeLine line = qualifiedLine();
        line.setIqcId(null);

        WmItemRecpt result = linkageService.createDraftWhenReady(
                notice(), Collections.singletonList(line), "tester");

        assertNull(result);
        verify(receiptService, never()).insertWmItemRecpt(any(WmItemRecpt.class));
        verify(menuNoticeService, never()).markUnread(MenuLinkageNoticePaths.ITEM_RECPT);
    }

    private WmArrivalNotice notice() {
        WmArrivalNotice notice = new WmArrivalNotice();
        notice.setNoticeId(1L);
        notice.setNoticeCode("ARR-001");
        notice.setPoCode("PO-001");
        notice.setVendorId(2L);
        notice.setVendorCode("V-001");
        notice.setVendorName("供应商");
        return notice;
    }

    private WmArrivalNoticeLine qualifiedLine() {
        WmArrivalNoticeLine line = new WmArrivalNoticeLine();
        line.setLineId(10L);
        line.setNoticeId(1L);
        line.setIqcCheck(UserConstants.YES);
        line.setIqcId(20L);
        line.setIqcCode("IQC-001");
        line.setItemId(3L);
        line.setItemCode("MAT-001");
        line.setItemName("原材料");
        line.setUnitOfMeasure("PCS");
        line.setQuantityArrival(new BigDecimal("10"));
        line.setQuantityQuanlified(new BigDecimal("8"));
        return line;
    }
}
