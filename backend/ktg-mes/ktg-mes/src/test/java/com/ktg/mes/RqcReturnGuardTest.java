package com.ktg.mes;

import com.ktg.common.core.domain.AjaxResult;
import com.ktg.mes.qc.controller.QcRqcController;
import com.ktg.mes.qc.domain.QcRqc;
import com.ktg.mes.qc.domain.QcTemplateProduct;
import com.ktg.mes.qc.service.IQcRqcService;
import com.ktg.mes.qc.service.IQcTemplateProductService;
import com.ktg.mes.wm.domain.WmRtIssue;
import com.ktg.mes.wm.domain.WmRtIssueLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.service.IWmRtIssueLineService;
import com.ktg.mes.wm.service.IWmRtIssueService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RqcReturnGuardTest {
    private QcRqcController controller;
    private PostingLockMapper locks;
    private IQcRqcService inspections;
    private IWmRtIssueLineService lines;
    private IWmRtIssueService headers;
    private IQcTemplateProductService templates;
    private QcRqc request;
    private WmRtIssueLine line;
    @Before public void setup() {
        controller = new QcRqcController(); locks = mock(PostingLockMapper.class);
        inspections = mock(IQcRqcService.class); lines = mock(IWmRtIssueLineService.class);
        headers = mock(IWmRtIssueService.class); templates = mock(IQcTemplateProductService.class);
        ReflectionTestUtils.setField(controller,"postingLockMapper",locks);
        ReflectionTestUtils.setField(controller,"qcRqcService",inspections);
        ReflectionTestUtils.setField(controller,"wmRtIssueLineService",lines);
        ReflectionTestUtils.setField(controller,"wmRtIssueService",headers);
        ReflectionTestUtils.setField(controller,"qcTemplateProductService",templates);
        when(locks.rqc(1L)).thenReturn("PREPARE"); when(locks.rqcSourceType(1L)).thenReturn("RT_ISSUE");
        when(locks.rqcRtIssueSource(1L)).thenReturn(2L); when(locks.rtIssue(2L)).thenReturn("UNCHECK");
        QcRqc stored = new QcRqc(); stored.setRqcId(1L); stored.setSourceDocType("RT_ISSUE");
        stored.setSourceDocId(2L); stored.setSourceLineId(3L); stored.setItemId(4L);
        when(inspections.selectQcRqcByRqcId(1L)).thenReturn(stored);
        line = new WmRtIssueLine(); line.setRtId(2L); line.setLineId(3L); line.setItemId(4L);
        line.setQuantityRt(BigDecimal.TEN); line.setQualityStatus("NT");
        when(lines.selectWmRtIssueLineByLineId(3L)).thenReturn(line);
        QcTemplateProduct template = new QcTemplateProduct(); template.setTemplateId(5L);
        when(templates.selectQcTemplateProductList(any(QcTemplateProduct.class))).thenReturn(Collections.singletonList(template));
        WmRtIssue parent = new WmRtIssue(); parent.setRtId(2L);
        when(headers.selectWmRtIssueByRtId(2L)).thenReturn(parent);
        when(inspections.updateQcRqc(any(QcRqc.class))).thenReturn(1);
        request = new QcRqc(); request.setRqcId(1L); request.setStatus("FINISHED");
        request.setQuantityCheck(BigDecimal.TEN); request.setQuantityQualified(BigDecimal.TEN);
        request.setQuantityUnqualified(BigDecimal.ZERO);
        request.setCheckResult("ACCEPT");
    }
    @Test public void allQualifiedUpdatesSourceLineAndParent() {
        AjaxResult response=controller.edit(request);
        assertEquals(200,response.get("code"));
        verify(lines).applyInspectionResult(line); assertEquals("OK",line.getQualityStatus());
        verify(headers).updateWmRtIssue(any(WmRtIssue.class));
    }
    @Test public void completedInspectionIsNotRepeated() {
        when(locks.rqc(1L)).thenReturn("FINISHED");
        AjaxResult response=controller.edit(request);
        assertNotEquals(200,response.get("code")); verify(lines,never()).applyInspectionResult(any());
    }
    @Test public void forgedOrIncompleteQuantityIsRejectedBeforeWrite() {
        request.setSourceDocId(99L); assertNotEquals(200,controller.edit(request).get("code"));
        request.setSourceDocId(null); request.setQuantityCheck(BigDecimal.ONE);
        assertNotEquals(200,controller.edit(request).get("code")); verify(lines,never()).applyInspectionResult(any());
    }
}
