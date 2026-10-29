package com.ktg.mes;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.domain.WmIssueDetail;
import com.ktg.mes.wm.domain.WmIssueLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.WmIssueDetailMapper;
import com.ktg.mes.wm.mapper.WmIssueLineMapper;
import com.ktg.mes.wm.service.impl.WmIssueDetailServiceImpl;
import com.ktg.mes.wm.service.impl.WmIssueLineServiceImpl;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

public class IssueMutationGuardTest {
    @Test(expected = ServiceException.class)
    public void finishedParentRejectsNewDetail() {
        WmIssueDetailServiceImpl service = new WmIssueDetailServiceImpl();
        PostingLockMapper locks = mock(PostingLockMapper.class);
        ReflectionTestUtils.setField(service, "postingLockMapper", locks);
        when(locks.issue(1L)).thenReturn("FINISHED");
        WmIssueDetail detail = new WmIssueDetail(); detail.setIssueId(1L); detail.setLineId(2L);
        service.insertWmIssueDetail(detail);
    }

    @Test(expected = ServiceException.class)
    public void cannotMoveDetailToAnotherDocument() {
        WmIssueDetailServiceImpl service = new WmIssueDetailServiceImpl();
        PostingLockMapper locks = mock(PostingLockMapper.class);
        WmIssueDetailMapper details = mock(WmIssueDetailMapper.class);
        ReflectionTestUtils.setField(service, "postingLockMapper", locks);
        ReflectionTestUtils.setField(service, "wmIssueDetailMapper", details);
        WmIssueDetail db = new WmIssueDetail(); db.setDetailId(3L); db.setIssueId(1L); db.setLineId(4L);
        when(details.selectWmIssueDetailByDetailId(3L)).thenReturn(db);
        when(locks.issue(1L)).thenReturn("APPROVING");
        when(locks.issueIdForDetail(3L)).thenReturn(1L);
        WmIssueDetail request = new WmIssueDetail(); request.setDetailId(3L); request.setIssueId(2L);
        service.updateWmIssueDetail(request);
    }

    @Test(expected = ServiceException.class)
    public void finishedParentRejectsLineUpdate() {
        WmIssueLineServiceImpl service = new WmIssueLineServiceImpl();
        PostingLockMapper locks = mock(PostingLockMapper.class);
        WmIssueLineMapper lines = mock(WmIssueLineMapper.class);
        ReflectionTestUtils.setField(service, "postingLockMapper", locks);
        ReflectionTestUtils.setField(service, "wmIssueLineMapper", lines);
        WmIssueLine db = new WmIssueLine(); db.setLineId(3L); db.setIssueId(1L);
        when(lines.selectWmIssueLineByLineId(3L)).thenReturn(db);
        when(locks.issue(1L)).thenReturn("FINISHED");
        service.updateWmIssueLine(db);
    }
}
