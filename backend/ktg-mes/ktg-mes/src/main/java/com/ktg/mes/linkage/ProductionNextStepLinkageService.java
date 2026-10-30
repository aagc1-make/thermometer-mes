package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.pro.domain.ProTask;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.service.IProTaskService;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.domain.WmIssueHeader;
import com.ktg.mes.wm.service.IWmIssueHeaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产侧"下一步该做什么"的提醒判断与领料草稿对账。
 *
 * 设计原则：**目标菜单里必须真的有可处理的单据**才点亮，
 * 避免出现"亮着蓝点但列表为空"的误导。
 */
@Service
public class ProductionNextStepLinkageService {

    private static final Logger log = LoggerFactory.getLogger(ProductionNextStepLinkageService.class);

    private final IProWorkorderService workorderService;
    private final IProTaskService taskService;
    private final IWmIssueHeaderService issueHeaderService;
    private final IssueDraftLinkageService issueDraftLinkageService;
    private final IssueDraftWorkorderService issueDraftWorkorderService;
    private final MenuLinkageNoticeService menuNoticeService;

    public ProductionNextStepLinkageService(IProWorkorderService workorderService,
                                            IProTaskService taskService,
                                            IWmIssueHeaderService issueHeaderService,
                                            IssueDraftLinkageService issueDraftLinkageService,
                                            IssueDraftWorkorderService issueDraftWorkorderService,
                                            MenuLinkageNoticeService menuNoticeService) {
        this.workorderService = workorderService;
        this.taskService = taskService;
        this.issueHeaderService = issueHeaderService;
        this.issueDraftLinkageService = issueDraftLinkageService;
        this.issueDraftWorkorderService = issueDraftWorkorderService;
        this.menuNoticeService = menuNoticeService;
    }

    /**
     * 原料入库后（afterCommit）调用：对账"已排产但还没有领料草稿"的工单，逐张在独立事务里补建。
     *
     * <p>关键约束：</p>
     * <ul>
     *   <li>只处理已排产（存在工序任务）的工单，未排产工单不因采购入库而生成草稿；</li>
     *   <li>每张工单独立事务（{@link IssueDraftWorkorderService#createForWorkorder} 为 REQUIRES_NEW），
     *       一张失败不使其它工单回滚；</li>
     *   <li>本方法是重试/补偿入口：重跑只会补建仍然缺失的草稿（唯一键幂等），不会重复建单。</li>
     * </ul>
     *
     * @param receiptId 触发本次对账的采购入库单 ID（可空；仅用于日志定位）
     * @param operator  操作人
     * @return 对账结果（新建/已存在/跳过/失败的数量与工单标识），失败项会在结果中明确体现
     */
    public IssueDraftReconcileResult ensureIssueDraftReady(Long receiptId, String operator) {
        List<ProWorkorder> workorders = listWorkordersNeedingIssueDraft();
        IssueDraftReconcileResult result = new IssueDraftReconcileResult();
        for (ProWorkorder wo : workorders) {
            try {
                IssueDraftWorkorderService.IssueDraftOutcome outcome =
                        issueDraftWorkorderService.createForWorkorder(wo.getWorkorderId(), operator);
                switch (outcome) {
                    case CREATED:
                        result.addCreated(wo.getWorkorderId());
                        break;
                    case EXISTED:
                        result.addExisted(wo.getWorkorderId());
                        break;
                    default:
                        result.addSkipped(wo.getWorkorderId());
                }
            } catch (Exception e) {
                // 单张工单失败只记日志、继续下一张；其它工单的独立事务不受影响。
                result.addFailed(wo.getWorkorderId());
                log.error("采购入库[{}]提交后，为工单[{}]生成领料草稿失败，可调用 /mes/pro/workorder/reconcileIssueDrafts 重试。operator={}",
                        receiptId, wo.getWorkorderId(), operator, e);
            }
        }
        if (result.getCreated() > 0) {
            menuNoticeService.markUnread(MenuLinkageNoticePaths.ISSUE);
        }
        return result;
    }

    /**
     * 列出"已确认、已排产、且还没有领料草稿"的工单。
     */
    public List<ProWorkorder> listWorkordersNeedingIssueDraft() {
        ProWorkorder query = new ProWorkorder();
        query.setStatus(UserConstants.ORDER_STATUS_CONFIRMED);
        List<ProWorkorder> workorders = workorderService.selectProWorkorderList(query);
        List<ProWorkorder> result = new ArrayList<>();
        if (workorders == null) {
            return result;
        }
        for (ProWorkorder wo : workorders) {
            // 未排产（无工序任务）的工单不生成领料草稿
            if (!hasTask(wo.getWorkorderId())) {
                continue;
            }
            WmIssueHeader headerQuery = new WmIssueHeader();
            headerQuery.setWorkorderId(wo.getWorkorderId());
            List<WmIssueHeader> existing = issueHeaderService.selectWmIssueHeaderList(headerQuery);
            if (existing == null || existing.isEmpty()) {
                result.add(wo);
            }
        }
        return result;
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
