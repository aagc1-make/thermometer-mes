package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.utils.DateUtils;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.domain.WmProductRecpt;
import com.ktg.mes.wm.domain.WmProductRecptLine;
import com.ktg.mes.wm.mapper.ProductReceiptGuardMapper;
import com.ktg.mes.wm.service.IWmProductRecptLineService;
import com.ktg.mes.wm.service.IWmProductRecptService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工单完工后，按「末工序合格产出 + 来源批次」生成产品入库草稿（成品仓）。
 *
 * <p>口径（本轮复审修正）：</p>
 * <ul>
 *   <li>来源 = 该工单产出行中 {@code item_id = 工单成品} 且 {@code quality_status='OK'} 的已放行产出，
 *       按批次汇总（成品只在该工单最后一道产出工序产生，半成品子工单不会命中）；</li>
 *   <li>**移除计划量/已生产数量兜底**：没有可靠合格产出时不生成可执行入库数量；</li>
 *   <li>可入库量 = 该批次合格产出 − 已执行入库累计 − 未执行草稿占用；</li>
 *   <li>按「工单 + 批次」幂等建草稿（每个来源批次一张草稿，支持分批产出/分批入库）；</li>
 *   <li>执行过账时再校验一次累计上限（见 {@code WmProductRecptController#execute}）；</li>
 *   <li>历史无产出来源/已有旧口径草稿（{@code AUTO:PRODUCT_RECPT:WORKORDER:&lt;id&gt;}）只读保留，不重复补建。</li>
 * </ul>
 */
@Service
public class ProductReceiptLinkageService {

    private static final Logger log = LoggerFactory.getLogger(ProductReceiptLinkageService.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final IProWorkorderService workorderService;
    private final IWmProductRecptService recptService;
    private final IWmProductRecptLineService recptLineService;
    private final ProductReceiptGuardMapper guardMapper;
    private final AutoCodeUtil autoCodeUtil;
    private final MenuLinkageNoticeService menuNoticeService;

    public ProductReceiptLinkageService(IProWorkorderService workorderService,
                                        IWmProductRecptService recptService,
                                        IWmProductRecptLineService recptLineService,
                                        ProductReceiptGuardMapper guardMapper,
                                        AutoCodeUtil autoCodeUtil,
                                        MenuLinkageNoticeService menuNoticeService) {
        this.workorderService = workorderService;
        this.recptService = recptService;
        this.recptLineService = recptLineService;
        this.guardMapper = guardMapper;
        this.autoCodeUtil = autoCodeUtil;
        this.menuNoticeService = menuNoticeService;
    }

    /**
     * 报工/IPQC 完成/手工完工后调用：为完工的顶层整机工单按批次生成产品入库草稿。
     *
     * @return 本次新建的草稿（可能为 null：未完工、子工单、无可靠产出、已存在草稿）
     */
    @Transactional
    public WmProductRecpt ensureDraftForFinishedWorkorder(Long workorderId, String operator) {
        if (workorderId == null) {
            return null;
        }
        ProWorkorder workorder = workorderService.selectProWorkorderByWorkorderId(workorderId);
        if (workorder == null || !UserConstants.ORDER_STATUS_FINISHED.equals(workorder.getStatus())) {
            return null;
        }
        // 半成品子工单：产出在线边库流转，不进成品仓
        if (workorder.getParentId() != null && workorder.getParentId() != 0L) {
            return null;
        }

        String legacyKey = "AUTO:PRODUCT_RECPT:WORKORDER:" + workorderId;
        // 兼容：已有旧口径草稿（按工单一张）时只读保留，不重复补建，避免同一产出重复入库
        if (guardMapper.countProductRecptByLinkageKey(legacyKey) > 0) {
            return findExisting(workorderId);
        }

        // 包装完成放行证据：终末工序(包装)必须有 FINISHED 报工，否则不生成入库草稿
        Long terminalProcessId = guardMapper.terminalProcessId(workorder.getProductId());
        if (terminalProcessId == null
                || guardMapper.countFinishedFeedbackAtProcess(workorderId, terminalProcessId) == 0) {
            return null;
        }
        // 产出来源工序应恰好一道；无或多道视为口径不确定，不建单
        List<Long> processes = guardMapper.produceProcessesForProduct(workorderId, workorder.getProductId());
        if (processes == null || processes.size() != 1) {
            return null;
        }
        Long processId = processes.get(0);

        List<Map<String, Object>> outputs = guardMapper.qualifiedOutputByBatch(
                workorderId, workorder.getProductId(), processId);
        if (outputs == null || outputs.isEmpty()) {
            // 无可靠合格产出：不生成可执行入库数量
            return null;
        }
        // 保守口径：包装报工无批次字段，无法把包装量可靠拆分到多个产出批次 → 多批次时阻断，转人工
        if (outputs.size() > 1) {
            log.warn("工单[{}]存在多批次合格产出，而包装报工无批次，无法可靠关联各批次包装量，需人工处理", workorderId);
            return null;
        }

        // 工单级包装放行上限：可入库 ≤ min(合格产出合计, 有效完成包装量)
        BigDecimal totalOutput = nvl(guardMapper.totalQualifiedOutput(workorderId, workorder.getProductId(), processId));
        BigDecimal packagingCap = guardMapper.completedPackagingQuantity(workorderId, terminalProcessId);
        if (packagingCap == null) {
            packagingCap = totalOutput;
        }
        BigDecimal cap = totalOutput.min(packagingCap);
        BigDecimal totalFinished = nvl(guardMapper.finishedProductRecptTotal(workorderId));
        BigDecimal totalAllowable = cap.subtract(totalFinished);

        WmProductRecpt lastCreated = null;
        for (Map<String, Object> batchOutput : outputs) {
            String batchCode = batchOutput.get("batchCode") == null ? "" : String.valueOf(batchOutput.get("batchCode"));
            BigDecimal produced = toDecimal(batchOutput.get("quantity"));
            if (produced == null || produced.compareTo(ZERO) <= 0) {
                continue;
            }
            BigDecimal finished = nvl(guardMapper.finishedProductRecptByBatch(workorderId, batchCode));
            BigDecimal occupied = nvl(guardMapper.occupiedProductRecptByBatch(workorderId, batchCode, null));
            BigDecimal available = produced.subtract(finished).subtract(occupied);
            // 包装放行上限再收紧：单批次场景下 available 不得突破工单级可入库余量
            available = available.min(totalAllowable);
            if (available.compareTo(ZERO) <= 0) {
                continue;
            }

            String linkageKey = legacyKey + ":BATCH:" + (batchCode.isEmpty() ? "NOBATCH" : batchCode);
            if (guardMapper.countProductRecptByLinkageKey(linkageKey) > 0) {
                continue; // 该来源批次已建草稿
            }

            WmProductRecpt recpt = buildReceipt(workorder, operator, linkageKey);
            try {
                recptService.insertWmProductRecpt(recpt);
            } catch (DuplicateKeyException concurrent) {
                continue; // 并发：唯一键兜底，跳过
            }

            WmProductRecptLine line = buildLine(recpt, workorder, batchOutput, batchCode, available, operator);
            recptLineService.insertWmProductRecptLine(line);
            lastCreated = recpt;
        }

        if (lastCreated != null) {
            try {
                menuNoticeService.markUnread(MenuLinkageNoticePaths.PRODUCT_RECPT);
            } catch (Exception ignore) {
                // 提醒失败不影响已生成的入库草稿
            }
        }
        return lastCreated;
    }

    private WmProductRecpt buildReceipt(ProWorkorder workorder, String operator, String linkageKey) {
        WmProductRecpt recpt = new WmProductRecpt();
        recpt.setRecptCode(autoCodeUtil.genSerialCode(UserConstants.PRODUCTRECPT_CODE, ""));
        recpt.setRecptName(workorder.getWorkorderName() + "产品入库");
        recpt.setWorkorderId(workorder.getWorkorderId());
        recpt.setWorkorderCode(workorder.getWorkorderCode());
        recpt.setWorkorderName(workorder.getWorkorderName());
        recpt.setItemId(workorder.getProductId());
        recpt.setItemCode(workorder.getProductCode());
        recpt.setItemName(workorder.getProductName());
        recpt.setSpecification(workorder.getProductSpc());
        recpt.setUnitOfMeasure(workorder.getUnitOfMeasure());
        recpt.setUnitName(workorder.getUnitName());
        recpt.setRecptDate(DateUtils.getNowDate());
        recpt.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        recpt.setAttr1(linkageKey);
        recpt.setCreateBy(operator);
        return recpt;
    }

    private WmProductRecptLine buildLine(WmProductRecpt recpt,
                                         ProWorkorder workorder,
                                         Map<String, Object> batchOutput,
                                         String batchCode,
                                         BigDecimal quantity,
                                         String operator) {
        WmProductRecptLine line = new WmProductRecptLine();
        line.setRecptId(recpt.getRecptId());
        line.setItemId(workorder.getProductId());
        line.setItemCode(workorder.getProductCode());
        line.setItemName(workorder.getProductName());
        line.setSpecification(workorder.getProductSpc());
        line.setUnitOfMeasure(workorder.getUnitOfMeasure());
        line.setUnitName(workorder.getUnitName());
        line.setQuantityRecived(quantity);
        line.setQuantity(quantity);
        line.setBatchId(batchOutput.get("batchId") == null ? null : Long.valueOf(String.valueOf(batchOutput.get("batchId"))));
        line.setBatchCode(batchCode.isEmpty() ? null : batchCode);
        line.setAttr1(recpt.getAttr1());
        line.setCreateBy(operator);
        return line;
    }

    private WmProductRecpt findExisting(Long workorderId) {
        WmProductRecpt query = new WmProductRecpt();
        query.setWorkorderId(workorderId);
        List<WmProductRecpt> list = recptService.selectWmProductRecptList(query);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.warn("产出数量无法解析：{}", value);
            return ZERO;
        }
    }
}
