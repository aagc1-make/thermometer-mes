package com.ktg.mes;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.qc.domain.QcRqc;
import com.ktg.mes.qc.service.IQcRqcService;
import com.ktg.mes.qc.service.IQcTemplateProductService;
import com.ktg.mes.qc.service.impl.QcRqcItemLineService;
import com.ktg.mes.qc.service.impl.RtIssueInspectionDraftService;
import com.ktg.mes.wm.domain.WmRtIssue;
import com.ktg.mes.wm.domain.WmRtIssueLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.service.IWmRtIssueLineService;
import com.ktg.mes.wm.service.IWmRtIssueService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ktg.mes.qc.domain.QcTemplateProduct;
import java.util.Collections;

/** 退料提交待检时生成 RQC 草稿：只对需检未检行、同一行不重复建单。 */
public class RtIssueInspectionDraftServiceTest {
    private PostingLockMapper locks;
    private IWmRtIssueService issues;
    private IWmRtIssueLineService lines;
    private IQcRqcService rqcs;
    private QcRqcItemLineService itemLines;
    private IQcTemplateProductService templates;
    private RtIssueInspectionDraftService service;

    @Before
    public void setup() {
        locks = mock(PostingLockMapper.class);
        issues = mock(IWmRtIssueService.class);
        lines = mock(IWmRtIssueLineService.class);
        rqcs = mock(IQcRqcService.class);
        templates = mock(IQcTemplateProductService.class);
        itemLines = mock(QcRqcItemLineService.class);
        AutoCodeUtil codes = mock(AutoCodeUtil.class);
        when(codes.genSerialCode(anyString(), isNull())).thenReturn("RQC-1");
        when(rqcs.insertQcRqc(any())).thenReturn(1);
        QcTemplateProduct tp = new QcTemplateProduct();
        tp.setTemplateId(555L);
        when(templates.selectQcTemplateProductList(any())).thenReturn(Collections.singletonList(tp));
        when(itemLines.generate(any())).thenReturn(2);
        service = new RtIssueInspectionDraftService(locks, issues, lines, rqcs, templates, codes, itemLines);
        WmRtIssue issue = new WmRtIssue();
        issue.setRtId(10L);
        issue.setRtCode("RT-1");
        when(issues.selectWmRtIssueByRtId(10L)).thenReturn(issue);
        when(locks.rtIssue(10L)).thenReturn(UserConstants.RTISSUE_STATUS_UNCHECK);
        when(locks.rqcIdForRtIssueLine(any())).thenReturn(null);
    }

    private WmRtIssueLine line(long id, String qcFlag, String quality, long item) {
        WmRtIssueLine l = new WmRtIssueLine();
        l.setLineId(id); l.setRtId(10L); l.setQcFlag(qcFlag); l.setQualityStatus(quality);
        l.setItemId(item); l.setItemCode("I" + item); l.setItemName("物料" + item);
        l.setQuantityRt(new BigDecimal("5"));
        return l;
    }

    @Test
    public void createsDraftOnlyForInspectedPendingLines() {
        when(lines.selectWmRtIssueLineList(any())).thenReturn(Arrays.asList(
                line(1L, "Y", UserConstants.QUALITY_STATUS_NOTTEST, 100L),
                line(2L, "N", UserConstants.QUALITY_STATUS_PASS, 101L),
                line(3L, "Y", UserConstants.QUALITY_STATUS_PASS, 102L)));
        int created = service.ensureDrafts(10L, "tester");
        assertEquals(1, created);
        ArgumentCaptor<QcRqc> cap = ArgumentCaptor.forClass(QcRqc.class);
        verify(rqcs, times(1)).insertQcRqc(cap.capture());
        QcRqc rqc = cap.getValue();
        assertEquals(Long.valueOf(10L), rqc.getSourceDocId());
        assertEquals(Long.valueOf(1L), rqc.getSourceLineId());
        assertEquals("RT_ISSUE", rqc.getSourceDocType());
        assertEquals(0, new BigDecimal("5").compareTo(rqc.getQuantityCheck()));
        assertEquals(UserConstants.ORDER_STATUS_PREPARE, rqc.getStatus());
        assertNull("合格/不合格数量必须留空，避免把未检数据写成合格", rqc.getQuantityQualified());
        assertEquals("模板存在时必须生成检测项目行", 555L, rqc.getTemplateId().longValue());
        verify(itemLines).generate(rqc);
    }

    @Test
    public void existingDraftIsReused() {
        when(lines.selectWmRtIssueLineList(any())).thenReturn(Collections.singletonList(
                line(1L, "Y", UserConstants.QUALITY_STATUS_NOTTEST, 100L)));
        when(locks.rqcIdForRtIssueLine(1L)).thenReturn(88L);
        assertEquals(0, service.ensureDrafts(10L, "tester"));
        verify(rqcs, never()).insertQcRqc(any());
    }

    @Test
    public void nonUncheckStatusDoesNothing() {
        when(locks.rtIssue(10L)).thenReturn(UserConstants.RTISSUE_STATUS_PREPARE);
        assertEquals(0, service.ensureDrafts(10L, "tester"));
        verifyNoInteractions(rqcs);
    }

    @Test
    public void missingTemplateFailsBeforeCreatingDraft() {
        when(lines.selectWmRtIssueLineList(any())).thenReturn(Collections.singletonList(
                line(1L, "Y", UserConstants.QUALITY_STATUS_NOTTEST, 100L)));
        when(templates.selectQcTemplateProductList(any())).thenReturn(Collections.emptyList());
        try {
            service.ensureDrafts(10L, "tester");
            fail("物料未配置检测模板时应直接报错");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("未配置检测模板"));
        }
        verify(rqcs, never()).insertQcRqc(any());
    }

    @Test(expected = ServiceException.class)
    public void missingIssueFails() {
        when(locks.rtIssue(10L)).thenReturn(UserConstants.RTISSUE_STATUS_UNCHECK);
        when(locks.rqcIdForRtIssueLine(any())).thenReturn(null);
        when(issues.selectWmRtIssueByRtId(10L)).thenReturn(null);
        service.ensureDrafts(10L, "tester");
    }
}
