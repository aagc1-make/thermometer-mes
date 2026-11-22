package com.ktg.mes;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.domain.WmRtIssue;
import com.ktg.mes.wm.domain.WmRtIssueDetail;
import com.ktg.mes.wm.domain.WmRtIssueLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.service.IStorageCoreService;
import com.ktg.mes.wm.service.IWmRtIssueDetailService;
import com.ktg.mes.wm.service.IWmRtIssueLineService;
import com.ktg.mes.wm.service.IWmRtIssueService;
import com.ktg.mes.wm.service.impl.RtIssueExecuteService;
import org.junit.Before;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RtIssueExecuteServiceTest {
    private PostingLockMapper locks;
    private IWmRtIssueService headers;
    private IWmRtIssueLineService lines;
    private IWmRtIssueDetailService details;
    private IStorageCoreService storage;
    private RtIssueExecuteService service;

    @Before public void setup() {
        locks = mock(PostingLockMapper.class); headers = mock(IWmRtIssueService.class);
        lines = mock(IWmRtIssueLineService.class); details = mock(IWmRtIssueDetailService.class);
        storage = mock(IStorageCoreService.class);
        service = new RtIssueExecuteService(locks, headers, lines, details, storage);
    }
    @Test(expected = ServiceException.class) public void finishedCannotExecuteAgain() {
        when(locks.rtIssue(1L)).thenReturn("FINISHED");
        service.execute(1L);
    }
    @Test(expected = ServiceException.class) public void pendingInspectionCannotExecute() {
        when(locks.rtIssue(1L)).thenReturn("UNCHECK");
        service.execute(1L);
    }
    @Test public void unqualifiedLineNeverReachesPosting() {
        when(locks.rtIssue(1L)).thenReturn("UNEXECUTE");
        WmRtIssueLine line = new WmRtIssueLine(); line.setRtId(1L); line.setLineId(2L);
        line.setItemId(3L); line.setQuantityRt(BigDecimal.ONE); line.setQualityStatus("NG");
        when(lines.selectWmRtIssueLineList(any(WmRtIssueLine.class))).thenReturn(Collections.singletonList(line));
        try { service.execute(1L); org.junit.Assert.fail("NG must be rejected"); }
        catch (ServiceException expected) { verifyNoInteractions(storage); }
    }
    @Test public void insufficientAllocationNeverReachesPosting() {
        when(locks.rtIssue(1L)).thenReturn("UNEXECUTE");
        WmRtIssueLine line = new WmRtIssueLine(); line.setRtId(1L); line.setLineId(2L);
        line.setItemId(3L); line.setBatchId(4L); line.setBatchCode("B");
        line.setQuantityRt(BigDecimal.TEN); line.setQualityStatus("OK");
        when(lines.selectWmRtIssueLineList(any(WmRtIssueLine.class))).thenReturn(Collections.singletonList(line));
        WmRtIssueDetail d = new WmRtIssueDetail(); d.setRtId(1L); d.setLineId(2L); d.setItemId(3L);
        d.setBatchId(4L); d.setBatchCode("B"); d.setQuantity(BigDecimal.ONE);
        d.setWarehouseId(1L); d.setLocationId(2L); d.setAreaId(3L);
        when(details.selectWmRtIssueDetailList(any(WmRtIssueDetail.class))).thenReturn(Collections.singletonList(d));
        try { service.execute(1L); org.junit.Assert.fail("partial allocation must be rejected"); }
        catch (ServiceException expected) { verifyNoInteractions(storage); }
    }
    @Test public void qualifiedAllocatedReturnIsPostedOnceAndFinished() {
        when(locks.rtIssue(1L)).thenReturn("UNEXECUTE");
        WmRtIssueLine line = new WmRtIssueLine(); line.setRtId(1L); line.setLineId(2L);
        line.setItemId(3L); line.setBatchId(4L); line.setBatchCode("B");
        line.setQuantityRt(BigDecimal.ONE); line.setQualityStatus("OK");
        when(lines.selectWmRtIssueLineList(any(WmRtIssueLine.class))).thenReturn(Collections.singletonList(line));
        WmRtIssueDetail d = new WmRtIssueDetail(); d.setRtId(1L); d.setLineId(2L); d.setItemId(3L);
        d.setBatchId(4L); d.setBatchCode("B"); d.setQuantity(BigDecimal.ONE);
        d.setWarehouseId(1L); d.setLocationId(2L); d.setAreaId(3L);
        when(details.selectWmRtIssueDetailList(any(WmRtIssueDetail.class))).thenReturn(Collections.singletonList(d));
        WmRtIssue header = new WmRtIssue(); header.setRtId(1L);
        when(headers.selectWmRtIssueByRtId(1L)).thenReturn(header);
        when(headers.updateWmRtIssue(header)).thenReturn(1);
        service.execute(1L);
        verify(storage, times(1)).processRtIssue(anyList());
        org.junit.Assert.assertEquals("FINISHED", header.getStatus());
    }
}
