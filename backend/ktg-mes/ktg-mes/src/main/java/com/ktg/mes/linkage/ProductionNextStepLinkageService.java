package com.ktg.mes.linkage;

import com.ktg.mes.pro.domain.ProTask;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.service.IProTaskService;
import com.ktg.mes.pro.service.IProWorkorderService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 生产侧"下一步该做什么"的提醒判断。
 *
 * 设计原则：只有目标菜单里真的有活可干时才点亮，避免出现"亮着蓝点但列表为空"的误导。
 */
@Service
public class ProductionNextStepLinkageService {

    private final IProWorkorderService workorderService;
    private final IProTaskService taskService;
    private final MenuLinkageNoticeService menuNoticeService;

    public ProductionNextStepLinkageService(IProWorkorderService workorderService,
                                            IProTaskService taskService,
                                            MenuLinkageNoticeService menuNoticeService) {
        this.workorderService = workorderService;
        this.taskService = taskService;
        this.menuNoticeService = menuNoticeService;
    }

    /**
     * 原料入库后：只有存在"已确认/已下达"的工单时才提示去领料。
     */
    public void remindIssueIfWorkorderReady() {
        ProWorkorder query = new ProWorkorder();
        query.setStatus(com.ktg.common.constant.UserConstants.ORDER_STATUS_CONFIRMED);
        List<ProWorkorder> workorders = workorderService.selectProWorkorderList(query);
        if (workorders == null || workorders.isEmpty()) {
            return;
        }
        menuNoticeService.markUnread(MenuLinkageNoticePaths.ISSUE);
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
