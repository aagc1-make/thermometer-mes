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

/**
 * 完成检验（edit 路径）的结论一致性：
 * - 全合格选 REJECT、全不合格选 ACCEPT、未填结论 → 拒绝，且不写库、不回族源、不改状态；
 * - 全合格 ACCEPT、全不合格 REJECT、混合 ACCEPT → 正常完成。
 */
public class RqcConclusionConsistencyTest {
    private QcRqcController controller;
    private PostingLockMapper locks;
    private IQcRqcService inspections;
    private IWmRtIssueLineService lines;
    private IWmRtIssueService headers;
    private WmRtIssueLine line;
    private QcRqc stored;
    private QcRqc request;

    @Before
    public void setup() {
        controller = new QcRqcController();
        locks = mock(PostingLockMapper.class);
        inspections = mock(IQcRqcService.class);
        lines = mock(IWmRtIssueLineService.class);
        headers = mock(IWmRtIssueService.class);
        IQcTemplateProductService templates = mock(IQcTemplateProductService.class);
        ReflectionTestUtils.setField(controller, "postingLockMapper", locks);
        ReflectionTestUtils.setField(controller, "qcRqcService", inspections);
        ReflectionTestUtils.setField(controller, "wmRtIssueLineService", lines);
        ReflectionTestUtils.setField(controller, "wmRtIssueService", headers);
        ReflectionTestUtils.setField(controller, "qcTemplateProductService", templates);

        when(locks.rqc(1L)).thenReturn("PREPARE");
        when(locks.rqcSourceType(1L)).thenReturn("RT_ISSUE");
        when(locks.rqcRtIssueSource(1L)).thenReturn(2L);
        when(locks.rtIssue(2L)).thenReturn("UNCHECK");
        stored = new QcRqc();
        stored.setRqcId(1L); stored.setSourceDocType("RT_ISSUE"); stored.setSourceDocId(2L);
        stored.setSourceLineId(3L); stored.setItemId(4L);
        when(inspections.selectQcRqcByRqcId(1L)).thenReturn(stored);
        line = new WmRtIssueLine();
        line.setRtId(2L); line.setLineId(3L); line.setItemId(4L);
        line.setQuantityRt(new BigDecimal("10")); line.setQualityStatus("NT");
        when(lines.selectWmRtIssueLineByLineId(3L)).thenReturn(line);
        QcTemplateProduct tp = new QcTemplateProduct(); tp.setTemplateId(5L);
        when(templates.selectQcTemplateProductList(any(QcTemplateProduct.class)))
                .thenReturn(Collections.singletonList(tp));
        WmRtIssue parent = new WmRtIssue(); parent.setRtId(2L);
        when(headers.selectWmRtIssueByRtId(2L)).thenReturn(parent);
        when(inspections.updateQcRqc(any(QcRqc.class))).thenReturn(1);

        request = new QcRqc();
        request.setRqcId(1L); request.setStatus("FINISHED");
        request.setQuantityCheck(new BigDecimal("10"));
        request.setQuantityQualified(new BigDecimal("10"));
        request.setQuantityUnqualified(BigDecimal.ZERO);
    }

    @Test
    public void allQualifiedWithRejectIsRejectedWithoutWriteback() {
        request.setCheckResult("REJECT");
        AjaxResult r = controller.edit(request);
        assertEquals(500, r.get("code"));
        assertTrue(String.valueOf(r.get("msg")).contains("结论与数量不一致"));
        verify(inspections, never()).updateQcRqc(any());
        verify(lines, never()).applyInspectionResult(any());
        verify(lines, never()).insertInspectionSplit(any());
        verify(headers, never()).updateWmRtIssue(any());
    }

    @Test
    public void allUnqualifiedWithAcceptIsRejected() {
        request.setQuantityQualified(BigDecimal.ZERO);
        request.setQuantityUnqualified(new BigDecimal("10"));
        request.setCheckResult("ACCEPT");
        AjaxResult r = controller.edit(request);
        assertEquals(500, r.get("code"));
        verify(inspections, never()).updateQcRqc(any());
        verify(lines, never()).applyInspectionResult(any());
    }

    @Test
    public void missingConclusionIsRejected() {
        request.setCheckResult(null);
        AjaxResult r = controller.edit(request);
        assertEquals(500, r.get("code"));
        assertTrue(String.valueOf(r.get("msg")).contains("请选择检验结论"));
        verify(inspections, never()).updateQcRqc(any());
    }

    @Test
    public void allQualifiedWithAcceptCompletes() {
        request.setCheckResult("ACCEPT");
        AjaxResult r = controller.edit(request);
        assertEquals(200, r.get("code"));
        verify(lines).applyInspectionResult(any());
        verify(inspections).updateQcRqc(any());
    }

    @Test
    public void allUnqualifiedWithRejectCompletes() {
        request.setQuantityQualified(BigDecimal.ZERO);
        request.setQuantityUnqualified(new BigDecimal("10"));
        request.setCheckResult("REJECT");
        AjaxResult r = controller.edit(request);
        assertEquals(200, r.get("code"));
        verify(inspections).updateQcRqc(any());
    }

    @Test
    public void mixedResultWithAcceptStillCompletes() {
        request.setQuantityQualified(new BigDecimal("4"));
        request.setQuantityUnqualified(new BigDecimal("6"));
        request.setCheckResult("ACCEPT");
        AjaxResult r = controller.edit(request);
        assertEquals(200, r.get("code"));
        verify(inspections).updateQcRqc(any());
    }
}
