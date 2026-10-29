package com.ktg.mes.linkage;

import com.ktg.mes.wm.domain.WmIssueHeader;
import com.ktg.mes.wm.service.IWmIssueHeaderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 单张工单的领料草稿生成事务（REQUIRES_NEW）。
 *
 * <p>存在的目的：采购入库提交后需要对账多张工单的领料草稿。若这些工单共用一个事务，
 * 任一张建单抛异常会把整个事务标记为 rollback-only，导致其它工单的成功结果也被回滚。
 * 这里把“一张工单”拆成独立事务，由编排层在非事务方法里逐张调用，并在其外层捕获异常，
 * 从而做到：一张失败，不影响其它工单。</p>
 */
@Service
public class IssueDraftWorkorderService {

    /** 单张工单对账结果。 */
    public enum IssueDraftOutcome {
        /** 新建了草稿 */
        CREATED,
        /** 已有草稿（预检命中，幂等） */
        EXISTED,
        /** 无需领料（无 BOM / 无外购料） */
        SKIPPED
    }

    private final IssueDraftLinkageService issueDraftLinkageService;
    private final IWmIssueHeaderService issueHeaderService;

    public IssueDraftWorkorderService(IssueDraftLinkageService issueDraftLinkageService,
                                      IWmIssueHeaderService issueHeaderService) {
        this.issueDraftLinkageService = issueDraftLinkageService;
        this.issueHeaderService = issueHeaderService;
    }

    /**
     * 在独立事务里为指定工单生成领料草稿（幂等）。
     *
     * @return 本次对账结果（新建/已存在/跳过）；建单失败时抛出异常，由编排层按工单隔离处理
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public IssueDraftOutcome createForWorkorder(Long workorderId, String operator) {
        // 信息性预检：区分“已存在”与“新建”；真正幂等仍由 createDraftIfNeeded + 唯一键兜底。
        WmIssueHeader query = new WmIssueHeader();
        query.setWorkorderId(workorderId);
        List<WmIssueHeader> existing = issueHeaderService.selectWmIssueHeaderList(query);
        if (existing != null && !existing.isEmpty()) {
            return IssueDraftOutcome.EXISTED;
        }
        WmIssueHeader draft = issueDraftLinkageService.createDraftIfNeeded(workorderId, operator);
        return draft == null ? IssueDraftOutcome.SKIPPED : IssueDraftOutcome.CREATED;
    }
}
