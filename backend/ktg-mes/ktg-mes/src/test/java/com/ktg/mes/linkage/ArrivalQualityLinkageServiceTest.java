package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.qc.domain.QcIqc;
import com.ktg.mes.qc.domain.QcTemplateIndex;
import com.ktg.mes.qc.domain.QcTemplateProduct;
import com.ktg.mes.qc.service.IQcIqcLineService;
import com.ktg.mes.qc.service.IQcIqcService;
import com.ktg.mes.qc.service.IQcTemplateIndexService;
import com.ktg.mes.qc.service.IQcTemplateProductService;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.service.IWmArrivalNoticeService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArrivalQualityLinkageServiceTest {

    private IQcIqcService iqcService;
    private IQcTemplateProductService templateProductService;
    private IQcTemplateIndexService templateIndexService;
    private IQcIqcLineService iqcLineService;
    private AutoCodeUtil autoCodeUtil;
    private MenuLinkageNoticeService menuNoticeService;
    private IWmArrivalNoticeService arrivalNoticeService;
    private ArrivalQualityLinkageService linkageService;

    @BeforeEach
    void setUp() {
        iqcService = mock(IQcIqcService.class);
        templateProductService = mock(IQcTemplateProductService.class);
        templateIndexService = mock(IQcTemplateIndexService.class);
        iqcLineService = mock(IQcIqcLineService.class);
        autoCodeUtil = mock(AutoCodeUtil.class);
        menuNoticeService = mock(MenuLinkageNoticeService.class);
        arrivalNoticeService = mock(IWmArrivalNoticeService.class);
        linkageService = new ArrivalQualityLinkageService(iqcService, templateProductService,
                templateIndexService, iqcLineService, autoCodeUtil, menuNoticeService, arrivalNoticeService);
    }

    @Test
    void createsOneDraftForARequiredArrivalLine() {
        WmArrivalNotice notice = notice();
        WmArrivalNoticeLine line = line();
        QcTemplateProduct template = new QcTemplateProduct();
        template.setTemplateId(7L);
        QcTemplateIndex index = new QcTemplateIndex();
        index.setIndexId(11L);

        when(iqcService.selectQcIqcList(any(QcIqc.class))).thenReturn(Collections.emptyList());
        when(templateProductService.selectQcTemplateProductList(any(QcTemplateProduct.class)))
                .thenReturn(Collections.singletonList(template));
        when(templateIndexService.selectQcTemplateIndexList(any(QcTemplateIndex.class)))
                .thenReturn(Collections.singletonList(index));
        when(autoCodeUtil.genSerialCode(UserConstants.QC_IQC_CODE, null)).thenReturn("IQC-001");
        when(iqcService.insertQcIqc(any(QcIqc.class))).thenAnswer(invocation -> {
            invocation.<QcIqc>getArgument(0).setIqcId(101L);
            return 1;
        });

        linkageService.createDrafts(notice, Collections.singletonList(line), "tester");

        ArgumentCaptor<QcIqc> captor = ArgumentCaptor.forClass(QcIqc.class);
        verify(iqcService).insertQcIqc(captor.capture());
        QcIqc draft = captor.getValue();
        assertEquals(UserConstants.ORDER_STATUS_PREPARE, draft.getStatus());
        assertEquals(UserConstants.SOURCE_DOC_TYPE_ARRIVALNOTICE, draft.getSourceDocType());
        assertEquals(line.getLineId(), draft.getSourceLineId());
        assertEquals(line.getQuantityArrival(), draft.getQuantityCheck());
        assertEquals(BigDecimal.ZERO, draft.getQuantityQualified());
        verify(iqcLineService).insertQcIqcLine(any());
        verify(menuNoticeService).markUnread(MenuLinkageNoticePaths.IQC);
    }

    @Test
    void repeatedSubmissionReusesExistingDraft() {
        QcIqc existing = new QcIqc();
        existing.setIqcId(101L);
        when(iqcService.selectQcIqcList(any(QcIqc.class)))
                .thenReturn(Collections.singletonList(existing));

        linkageService.createDrafts(notice(), Collections.singletonList(line()), "tester");

        verify(iqcService, never()).insertQcIqc(any(QcIqc.class));
        verify(autoCodeUtil, never()).genSerialCode(UserConstants.QC_IQC_CODE, null);
        verify(menuNoticeService, never()).markUnread(MenuLinkageNoticePaths.IQC);
    }

    private WmArrivalNotice notice() {
        WmArrivalNotice notice = new WmArrivalNotice();
        notice.setNoticeId(1L);
        notice.setNoticeCode("ARR-001");
        notice.setVendorId(2L);
        notice.setVendorCode("V-001");
        notice.setVendorName("供应商");
        return notice;
    }

    private WmArrivalNoticeLine line() {
        WmArrivalNoticeLine line = new WmArrivalNoticeLine();
        line.setLineId(10L);
        line.setNoticeId(1L);
        line.setIqcCheck(UserConstants.YES);
        line.setItemId(3L);
        line.setItemCode("MAT-001");
        line.setItemName("原材料");
        line.setUnitOfMeasure("PCS");
        line.setQuantityArrival(new BigDecimal("10"));
        return line;
    }
}
