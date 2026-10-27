package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.pro.domain.ProTask;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.service.IProTaskService;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.domain.WmIssueHeader;
import com.ktg.mes.wm.service.IWmIssueHeaderService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 生产侧"下一步该做什么"的提醒判断。
 *
 * 设计原则：**目标菜单里必须真的有可处理的单据**才点亮，
 * 避免出现"亮着蓝点但列表为空"的误导。
 */
@Service
public class ProductionNextStepLinkageService {

    private final IProWorkorderService workorderService;
    private final IProTaskService taskService;
    private final IWmIssueHeaderService issueHeaderService;
    private final IssueDraftLinkageService issueDraftLinkageService;
    private final MenuLinkageNoticeService menuNoticeService;

    public ProductionNextStepLinkageService(IProWorkorderService workorderService,
                                            IProTaskService taskService,
                                            IWmIssueHeaderService issueHeaderService,
                                            IssueDraftLinkageService issueDraftLinkageService,
                                            MenuLinkageNoticeService menuNoticeService) {
        this.workorderService = workorderService;
        this.taskService = taskService;
        this.issueHeaderService = issueHeaderService;
        this.issueDraftLinkageService = issueDraftLinkageService;
        this.menuNoticeService = menuNoticeService;
    }

    /**
     * 原料入库后调用：为"已确认但还没有领料单"的工单补建领料草稿，
     * 确有草稿存在时才点亮「生产领料」。
     *
     * 这样蓝点亮起时，列表里一定有单可处理（不会再出现"点进去是空的"）。
     *
     * @return 是否存在可处理的领料草稿
     */
    public boolean ensureIssueDraftReady(String operator) {
        ProWorkorder query = new ProWorkorder();
        query.setStatus(UserConstants.ORDER_STATUS_CONFIRMED);
        List<ProWorkorder> workorders = workorderService.selectProWorkorderList(query);
        if (workorders == null || workorders.isEmpty()) {
            return false;
        }
        boolean hasDraft = false;
        for (ProWorkorder wo : workorders) {
            try {
                WmIssueHeader draft = issueDraftLinkageService.createDraftIfNeeded(wo.getWorkorderId(), operator);
                if (draft != null && UserConstants.ORDER_STATUS_PREPARE.equals(draft.getStatus())) {
                    hasDraft = true;
                }
            } catch (Exception ignore) {
                // 单个工单建草稿失败不影响其它工单
            }
        }
        if (hasDraft) {
            menuNoticeService.markUnread(MenuLinkageNoticePaths.ISSUE);
        }
        return hasDraft;
    }

    /**
     * 工单确认后调用：该工单还没有排产任务时，点亮「生产排产」。
     */
    public void remindScheduleIfNoTask(Long workorderId) {
        if (workorderId == null) {
            return;
        }
        if (hasTask(workorderId)) {
            return;
        }
        menuNoticeService.markUnread(MenuLinkageNoticePaths.SCHEDULE);
    }

    /**
     * 排产保存后调用：如果所有"已确认"的工单都已经有任务，就清掉「生产排产」的蓝点。
     */
    public void clearScheduleIfAllScheduled() {
        ProWorkorder query = new ProWorkorder();
        query.setStatus(UserConstants.ORDER_STATUS_CONFIRMED);
        List<ProWorkorder> workorders = workorderService.selectProWorkorderList(query);
        if (workorders != null) {
            for (ProWorkorder wo : workorders) {
                if (!hasTask(wo.getWorkorderId())) {
                    return; // 还有工单没排产，保留蓝点
                }
            }
        }
        menuNoticeService.markRead(MenuLinkageNoticePaths.SCHEDULE);
    }

    private boolean hasTask(Long workorderId) {
        ProTask query = new ProTask();
        query.setWorkorderId(workorderId);
        List<ProTask> tasks = taskService.selectProTaskList(query);
        return tasks != null && !tasks.isEmpty();
    }

    /**
     * 领料后：只有该工单确实已经排产（存在工序任务）时才提示去报工。
     */
    public void remindFeedbackIfTaskReady(Long workorderId) {
        if (workorderId == null) {
            return;
        }
        ProTask query = new ProTask();
        query.setWorkorderId(workorderId);
        List<ProTask> tasks = taskService.selectProTaskList(query);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        menuNoticeService.markUnread(MenuLinkageNoticePaths.FEEDBACK);
    }
}
