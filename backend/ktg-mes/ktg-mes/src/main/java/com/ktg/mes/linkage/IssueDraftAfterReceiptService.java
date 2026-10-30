package com.ktg.mes.linkage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 采购入库提交后的领料草稿对账入口（afterCommit 边界）。
 *
 * <p>本类**不带事务**，只做两件事：</p>
 * <ol>
 *   <li>把请求转交给 {@link ProductionNextStepLinkageService#ensureIssueDraftReady}，
 *       由后者逐张工单走 {@link IssueDraftWorkorderService}（REQUIRES_NEW）独立事务；</li>
 *   <li>在最外层捕获任何残余异常，保证采购入库已经提交时，接口不会因为后续草稿失败而报错。</li>
 * </ol>
 *
 * <p>这样拆分的意义：事务注解必须经代理才生效。入库事务的 afterCommit 回调里，
 * 通过注入的代理调用本 Bean → 本 Bean 再调用另一个带 REQUIRES_NEW 的 Bean，逐张工单各成一个事务。</p>
 */
@Service
public class IssueDraftAfterReceiptService {

    private static final Logger log = LoggerFactory.getLogger(IssueDraftAfterReceiptService.class);

    private final ProductionNextStepLinkageService productionNextStepLinkageService;

    public IssueDraftAfterReceiptService(ProductionNextStepLinkageService productionNextStepLinkageService) {
        this.productionNextStepLinkageService = productionNextStepLinkageService;
    }

    /**
     * 入库已提交后调用：对账领料草稿。失败只记录日志（含入库单 ID），不向上抛出，
     * 因此不影响入库接口结果；可调用 /mes/pro/workorder/reconcileIssueDrafts 幂等重试。
     *
     * @param receiptId 已提交的采购入库单 ID
     * @param operator  操作人
     */
    public void prepareAfterReceipt(Long receiptId, String operator) {
        try {
            productionNextStepLinkageService.ensureIssueDraftReady(receiptId, operator);
        } catch (RuntimeException e) {
            log.error("采购入库[{}]已提交，但领料草稿对账整体失败，可调用 /mes/pro/workorder/reconcileIssueDrafts 重试。operator={}",
                    receiptId, operator, e);
        }
    }
}
