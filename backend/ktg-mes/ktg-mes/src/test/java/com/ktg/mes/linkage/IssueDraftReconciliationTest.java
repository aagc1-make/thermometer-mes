package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.pro.domain.ProTask;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.service.IProTaskService;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.domain.WmIssueHeader;
import com.ktg.mes.wm.service.IWmIssueHeaderService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 采购入库后领料草稿对账：逐张工单独立事务 + 排产过滤 + 幂等重试。
 */
class IssueDraftReconciliationTest {

    private ProWorkorder workorder(Long id) {
        ProWorkorder wo = new ProWorkorder();
        wo.setWorkorderId(id);
        wo.setWorkorderCode("WO-" + id);
        wo.setStatus(UserConstants.ORDER_STATUS_CONFIRMED);
        return wo;
    }

    private ProductionNextStepLinkageService service(
            IProWorkorderService workorderService,
            IProTaskService taskService,
            IWmIssueHeaderService issueHeaderService,
            IssueDraftWorkorderService issueDraftWorkorderService) {
        return new ProductionNextStepLinkageService(workorderService, taskService, issueHeaderService,
                mock(IssueDraftLinkageService.class), issueDraftWorkorderService, mock(MenuLinkageNoticeService.class));
    }

    @Test
    void filtersOutUnscheduledAndAlreadyDraftedWorkorders() {
        IProWorkorderService woService = mock(IProWorkorderService.class);
        when(woService.selectProWorkorderList(any(ProWorkorder.class)))
                .thenReturn(Arrays.asList(workorder(1L), workorder(2L), workorder(3L)));

        IProTaskService taskService = mock(IProTaskService.class);
        when(taskService.selectProTaskList(any(ProTask.class))).thenAnswer(inv -> {
            Long woId = inv.<ProTask>getArgument(0).getWorkorderId();
            // 工单2未排产
            return Long.valueOf(2L).equals(woId) ? Collections.emptyList() : Collections.singletonList(new ProTask());
        });

        IWmIssueHeaderService headerService = mock(IWmIssueHeaderService.class);
        when(headerService.selectWmIssueHeaderList(any(WmIssueHeader.class))).thenAnswer(inv -> {
            Long woId = inv.<WmIssueHeader>getArgument(0).getWorkorderId();
            // 工单3已有草稿
            return Long.valueOf(3L).equals(woId) ? Collections.singletonList(new WmIssueHeader()) : Collections.emptyList();
        });

        ProductionNextStepLinkageService svc = service(woService, taskService, headerService, mock(IssueDraftWorkorderService.class));
        List<ProWorkorder> result = svc.listWorkordersNeedingIssueDraft();

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(1L), result.get(0).getWorkorderId());
    }

    @Test
    void oneWorkorderFailureDoesNotBlockOthersAndDoesNotPropagate() {
        IProWorkorderService woService = mock(IProWorkorderService.class);
        when(woService.selectProWorkorderList(any(ProWorkorder.class)))
                .thenReturn(Arrays.asList(workorder(1L), workorder(2L)));

        IProTaskService taskService = mock(IProTaskService.class);
        when(taskService.selectProTaskList(any(ProTask.class))).thenReturn(Collections.singletonList(new ProTask()));

        IWmIssueHeaderService headerService = mock(IWmIssueHeaderService.class);
        when(headerService.selectWmIssueHeaderList(any(WmIssueHeader.class))).thenReturn(Collections.emptyList());

        IssueDraftWorkorderService issueDraftWorkorderService = mock(IssueDraftWorkorderService.class);
        when(issueDraftWorkorderService.createForWorkorder(anyLong(), any()))
                .thenThrow(new RuntimeException("工单1建单失败"))
                .thenReturn(IssueDraftWorkorderService.IssueDraftOutcome.CREATED);

        MenuLinkageNoticeService noticeService = mock(MenuLinkageNoticeService.class);
        ProductionNextStepLinkageService svc = new ProductionNextStepLinkageService(woService, taskService, headerService,
                mock(IssueDraftLinkageService.class), issueDraftWorkorderService, noticeService);

        IssueDraftReconcileResult result = assertDoesNotThrow(() -> svc.ensureIssueDraftReady(9L, "op"));

        assertEquals(1, result.getCreated());
        assertEquals(1, result.getFailed());
        assertEquals(Long.valueOf(1L), result.getFailedWorkorderIds().get(0));
        verify(issueDraftWorkorderService, times(2)).createForWorkorder(anyLong(), any());
        verify(noticeService).markUnread(MenuLinkageNoticePaths.ISSUE);
    }
}
