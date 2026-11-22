package com.ktg.mes;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.domain.WmRtIssueLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.WmRtIssueLineMapper;
import com.ktg.mes.wm.service.impl.WmRtIssueLineServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 退料行质量状态防绕过：
 * - 请求省略 qcFlag 时以数据库需检标志为准；
 * - 需检行不能通过编辑直接变成合格；
 * - 已检验（或有检验单）的行，物料/批次/数量冻结；
 * - 免检行（qcFlag=N）保持原有按退料类型判定。
 */
public class RtIssueQualityGuardTest {
    private WmRtIssueLineMapper mapper;
    private PostingLockMapper locks;
    private WmRtIssueLineServiceImpl service;

    @Before
    public void setup() throws Exception {
        mapper = mock(WmRtIssueLineMapper.class);
        locks = mock(PostingLockMapper.class);
        service = new WmRtIssueLineServiceImpl();
        set(service, "wmRtIssueLineMapper", mapper);
        set(service, "postingLockMapper", locks);
        when(locks.rtIssue(any())).thenReturn(UserConstants.RTISSUE_STATUS_PREPARE);
        when(locks.rtIssueIdForLine(any())).thenReturn(10L);
        when(mapper.updateWmRtIssueLine(any())).thenReturn(1);
        when(mapper.insertWmRtIssueLine(any())).thenReturn(1);
    }

    private void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private WmRtIssueLine db(long id, String qcFlag, String quality, String qty, long itemId, long batchId) {
        WmRtIssueLine l = new WmRtIssueLine();
        l.setLineId(id); l.setRtId(10L); l.setQcFlag(qcFlag); l.setQualityStatus(quality);
        l.setQuantityRt(new BigDecimal(qty)); l.setItemId(itemId); l.setBatchId(batchId);
        l.setBatchCode("B" + batchId);
        return l;
    }

    /** 需检行：请求省略 qcFlag 且不传质量状态，编辑后仍必须是未检。 */
    @Test
    public void omittingQcFlagCannotTurnInspectionLineIntoPass() {
        when(mapper.selectWmRtIssueLineByLineId(1L)).thenReturn(db(1L, "Y", UserConstants.QUALITY_STATUS_NOTTEST, "5", 100L, 200L));
        WmRtIssueLine req = new WmRtIssueLine();
        req.setLineId(1L); req.setRemark("备注");
        service.updateWmRtIssueLine(req);
        assertEquals(UserConstants.QUALITY_STATUS_NOTTEST, req.getQualityStatus());
        assertEquals("Y", req.getQcFlag());
    }

    /** 需检行：请求伪造合格状态，编辑后被强制回未检。 */
    @Test
    public void forgedPassStatusIsOverwrittenForInspectionLine() {
        when(mapper.selectWmRtIssueLineByLineId(1L)).thenReturn(db(1L, "Y", UserConstants.QUALITY_STATUS_NOTTEST, "5", 100L, 200L));
        WmRtIssueLine req = new WmRtIssueLine();
        req.setLineId(1L); req.setQcFlag("N");
        req.setQualityStatus(UserConstants.QUALITY_STATUS_PASS);
        service.updateWmRtIssueLine(req);
        assertEquals(UserConstants.QUALITY_STATUS_NOTTEST, req.getQualityStatus());
        assertEquals("Y", req.getQcFlag());
    }

    /** 已检验行：修改数量被拒绝，检验结论不被重算。 */
    @Test
    public void inspectedLineQuantityIsFrozen() {
        when(mapper.selectWmRtIssueLineByLineId(1L)).thenReturn(db(1L, "Y", UserConstants.QUALITY_STATUS_PASS, "5", 100L, 200L));
        WmRtIssueLine req = new WmRtIssueLine();
        req.setLineId(1L); req.setQuantityRt(new BigDecimal("3"));
        try {
            service.updateWmRtIssueLine(req);
            fail("已检验行数量不应被修改");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("已完成检验"));
        }
    }

    /** 已检验行：修改物料或批次被拒绝。 */
    @Test
    public void inspectedLineItemAndBatchAreFrozen() {
        when(mapper.selectWmRtIssueLineByLineId(1L)).thenReturn(db(1L, "Y", UserConstants.QUALITY_STATUS_NOTGOOD, "5", 100L, 200L));
        WmRtIssueLine item = new WmRtIssueLine();
        item.setLineId(1L); item.setItemId(101L);
        try { service.updateWmRtIssueLine(item); fail("物料不应被修改"); } catch (ServiceException e) { assertTrue(e.getMessage().contains("已完成检验")); }
        WmRtIssueLine batch = new WmRtIssueLine();
        batch.setLineId(1L); batch.setBatchId(201L); batch.setBatchCode("B201");
        try { service.updateWmRtIssueLine(batch); fail("批次不应被修改"); } catch (ServiceException e) { assertTrue(e.getMessage().contains("已完成检验")); }
    }

    /** 待检但已有检验单：同样冻结关键字段。 */
    @Test
    public void pendingLineWithInspectionIsFrozen() {
        when(mapper.selectWmRtIssueLineByLineId(1L)).thenReturn(db(1L, "Y", UserConstants.QUALITY_STATUS_NOTTEST, "5", 100L, 200L));
        when(locks.rqcIdForRtIssueLine(1L)).thenReturn(77L);
        WmRtIssueLine req = new WmRtIssueLine();
        req.setLineId(1L); req.setQuantityRt(new BigDecimal("4"));
        try {
            service.updateWmRtIssueLine(req);
            fail("已有检验单的待检行不应改数量");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("已有检验单"));
        }
    }

    /** 免检行（qcFlag=N）：编辑保持调用方给定的合格状态，不被强制成未检。 */
    @Test
    public void exemptLineKeepsPassStatus() {
        when(mapper.selectWmRtIssueLineByLineId(1L)).thenReturn(db(1L, "N", UserConstants.QUALITY_STATUS_PASS, "5", 100L, 200L));
        WmRtIssueLine req = new WmRtIssueLine();
        req.setLineId(1L); req.setQualityStatus(UserConstants.QUALITY_STATUS_PASS); req.setQuantityRt(new BigDecimal("5"));
        service.updateWmRtIssueLine(req);
        assertEquals(UserConstants.QUALITY_STATUS_PASS, req.getQualityStatus());
        assertEquals("N", req.getQcFlag());
    }

    /** 新增需检行：请求携带合格状态也被强制为未检。 */
    @Test
    public void insertInspectionLineForcesNotTested() {
        WmRtIssueLine req = new WmRtIssueLine();
        req.setRtId(10L); req.setQcFlag("Y");
        req.setQualityStatus(UserConstants.QUALITY_STATUS_PASS);
        service.insertWmRtIssueLine(req);
        assertEquals(UserConstants.QUALITY_STATUS_NOTTEST, req.getQualityStatus());
    }

    /** 检验回写通道：允许更新数量与结论（供 RQC 完成使用）。 */
    @Test
    public void inspectionWriteBackAllowsQuantityChange() {
        when(mapper.selectWmRtIssueLineByLineId(1L)).thenReturn(db(1L, "Y", UserConstants.QUALITY_STATUS_NOTTEST, "5", 100L, 200L));
        WmRtIssueLine line = db(1L, "Y", UserConstants.QUALITY_STATUS_PASS, "5", 100L, 200L);
        line.setQuantityRt(new BigDecimal("4"));
        service.applyInspectionResult(line);
        assertEquals(UserConstants.QUALITY_STATUS_PASS, line.getQualityStatus());
    }
}
