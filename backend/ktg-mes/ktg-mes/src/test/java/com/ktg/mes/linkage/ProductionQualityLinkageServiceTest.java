package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.pro.domain.ProFeedback;
import com.ktg.mes.qc.domain.QcIpqc;
import com.ktg.mes.qc.domain.QcTemplate;
import com.ktg.mes.qc.domain.QcTemplateIndex;
import com.ktg.mes.qc.service.IQcIpqcLineService;
import com.ktg.mes.qc.service.IQcIpqcService;
import com.ktg.mes.qc.service.IQcTemplateIndexService;
import com.ktg.mes.qc.service.IQcTemplateService;
import com.ktg.mes.wm.domain.WmProductProduce;
import com.ktg.mes.wm.domain.WmProductProduceLine;
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

class ProductionQualityLinkageServiceTest {

    private IQcIpqcService ipqcService;
    private IQcTemplateService templateService;
    private IQcTemplateIndexService templateIndexService;
    private IQcIpqcLineService ipqcLineService;
    private AutoCodeUtil autoCodeUtil;
    private MenuLinkageNoticeService menuNoticeService;
    private ProductionQualityLinkageService linkageService;

    @BeforeEach
    void setUp() {
        ipqcService = mock(IQcIpqcService.class);
        templateService = mock(IQcTemplateService.class);
        templateIndexService = mock(IQcTemplateIndexService.class);
        ipqcLineService = mock(IQcIpqcLineService.class);
        autoCodeUtil = mock(AutoCodeUtil.class);
        menuNoticeService = mock(MenuLinkageNoticeService.class);
        linkageService = new ProductionQualityLinkageService(ipqcService, templateService,
                templateIndexService, ipqcLineService, autoCodeUtil, menuNoticeService);
    }

    @Test
    void createsIpqcDraftForFeedbackOutputLine() {
        QcTemplate template = new QcTemplate();
        template.setTemplateId(7L);
        when(ipqcService.selectQcIpqcList(any(QcIpqc.class))).thenReturn(Collections.emptyList());
        when(templateService.selectQcTemplateByProductAndQcType(any(QcTemplate.class)))
                .thenReturn(template);
        when(templateIndexService.selectQcTemplateIndexList(any(QcTemplateIndex.class)))
                .thenReturn(Collections.emptyList());
        when(autoCodeUtil.genSerialCode(UserConstants.QC_PQC_CODE, null)).thenReturn("IPQC-001");

        linkageService.createDraft(feedback(), produce(), line(), "tester");

        ArgumentCaptor<QcIpqc> captor = ArgumentCaptor.forClass(QcIpqc.class);
        verify(ipqcService).insertQcIpqc(captor.capture());
        assertEquals(UserConstants.SOURCE_DOC_TYPE_FEEDBACK, captor.getValue().getSourceDocType());
        assertEquals(100L, captor.getValue().getSourceDocId());
        assertEquals(101L, captor.getValue().getSourceLineId());
        assertEquals(new BigDecimal("10"), captor.getValue().getQuantityCheck());
        assertEquals(UserConstants.ORDER_STATUS_PREPARE, captor.getValue().getStatus());
        verify(menuNoticeService).markUnread(MenuLinkageNoticePaths.IPQC);
    }

    @Test
    void repeatedFeedbackExecutionReusesIpqcDraft() {
        when(ipqcService.selectQcIpqcList(any(QcIpqc.class)))
                .thenReturn(Collections.singletonList(new QcIpqc()));

        linkageService.createDraft(feedback(), produce(), line(), "tester");

        verify(ipqcService, never()).insertQcIpqc(any(QcIpqc.class));
        verify(menuNoticeService, never()).markUnread(MenuLinkageNoticePaths.IPQC);
    }

    private ProFeedback feedback() {
        ProFeedback feedback = new ProFeedback();
        feedback.setRecordId(1L);
        feedback.setFeedbackCode("FB-001");
        feedback.setWorkorderId(2L);
        feedback.setWorkorderCode("WO-001");
        feedback.setWorkorderName("工单");
        feedback.setTaskId(3L);
        feedback.setTaskCode("TASK-001");
        feedback.setWorkstationId(4L);
        feedback.setWorkstationCode("WS-001");
        feedback.setWorkstationName("工作站");
        feedback.setProcessId(5L);
        feedback.setProcessCode("P-001");
        feedback.setProcessName("工序");
        return feedback;
    }

    private WmProductProduce produce() {
        WmProductProduce produce = new WmProductProduce();
        produce.setRecordId(100L);
        return produce;
    }

    private WmProductProduceLine line() {
        WmProductProduceLine line = new WmProductProduceLine();
        line.setLineId(101L);
        line.setRecordId(100L);
        line.setItemId(6L);
        line.setItemCode("FG-001");
        line.setItemName("成品");
        line.setUnitOfMeasure("PCS");
        line.setQuantityProduce(new BigDecimal("10"));
        return line;
    }
}
