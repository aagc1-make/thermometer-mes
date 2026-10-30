package com.ktg.mes.wm.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.linkage.FormLinkagePolicy;
import com.ktg.mes.linkage.MenuLinkageNoticePaths;
import com.ktg.mes.linkage.MenuLinkageNoticeService;
import com.ktg.mes.linkage.ProductionNextStepLinkageService;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmItemRecpt;
import com.ktg.mes.wm.domain.WmItemRecptLine;
import com.ktg.mes.wm.domain.tx.ItemRecptTxBean;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.ReceiptGuardMapper;
import com.ktg.mes.wm.service.IStorageCoreService;
import com.ktg.mes.wm.service.IWmArrivalNoticeLineService;
import com.ktg.mes.wm.service.IWmArrivalNoticeService;
import com.ktg.mes.wm.service.IWmItemRecptDetailService;
import com.ktg.mes.wm.service.IWmItemRecptLineService;
import com.ktg.mes.wm.service.IWmItemRecptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 采购入库单的状态机与过账校验（PC 与移动端共用）。
 *
 * <p>本服务把原先分散在两个 Controller 里的“确认/编辑/执行”编排收敛到一处，
 * 保证同一业务动作在服务端只有一套规则：</p>
 * <ul>
 *   <li>所有写操作先对主单加 {@code for update} 行锁，再根据数据库中的真实状态判断流转；</li>
 *   <li>状态流转使用条件更新，重复请求或并发请求只有一次成功；</li>
 *   <li>过账前以数据库中的来源行、检验单为准核对数量，不接受前端传入的合格量或检验结论；</li>
 *   <li>已执行/已取消单据不能被普通接口改回可执行状态。</li>
 * </ul>
 */
@Service
public class ItemRecptWorkflowService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /** 允许通过普通编辑提交的目标状态。 */
    private static final Map<String, Set<String>> EDIT_TRANSITIONS = new HashMap<>();

    static {
        EDIT_TRANSITIONS.put(UserConstants.ORDER_STATUS_PREPARE,
                set(UserConstants.ORDER_STATUS_PREPARE, UserConstants.ORDER_STATUS_APPROVING));
        EDIT_TRANSITIONS.put(UserConstants.ORDER_STATUS_CONFIRMED,
                set(UserConstants.ORDER_STATUS_CONFIRMED, UserConstants.ORDER_STATUS_APPROVING));
        EDIT_TRANSITIONS.put(UserConstants.ORDER_STATUS_APPROVING,
                set(UserConstants.ORDER_STATUS_APPROVING, UserConstants.ORDER_STATUS_APPROVED,
                        UserConstants.ORDER_STATUS_CANCELED));
        EDIT_TRANSITIONS.put(UserConstants.ORDER_STATUS_APPROVED,
                set(UserConstants.ORDER_STATUS_APPROVED, UserConstants.ORDER_STATUS_CANCELED));
    }

    private static Set<String> set(String... values) {
        Set<String> result = new HashSet<>();
        Collections.addAll(result, values);
        return result;
    }

    private final PostingLockMapper postingLockMapper;
    private final ReceiptGuardMapper guardMapper;
    private final IWmItemRecptService receiptService;
    private final IWmItemRecptLineService receiptLineService;
    private final IWmItemRecptDetailService receiptDetailService;
    private final IWmArrivalNoticeService arrivalNoticeService;
    private final IWmArrivalNoticeLineService arrivalNoticeLineService;
    private final IStorageCoreService storageCoreService;
    private final FormLinkagePolicy formLinkagePolicy;
    private final MenuLinkageNoticeService menuLinkageNoticeService;
    private final ProductionNextStepLinkageService productionNextStepLinkageService;
    private final com.ktg.mes.linkage.IssueDraftAfterReceiptService issueDraftAfterReceiptService;

    public ItemRecptWorkflowService(PostingLockMapper postingLockMapper,
                                    ReceiptGuardMapper guardMapper,
                                    IWmItemRecptService receiptService,
                                    IWmItemRecptLineService receiptLineService,
                                    IWmItemRecptDetailService receiptDetailService,
                                    IWmArrivalNoticeService arrivalNoticeService,
                                    IWmArrivalNoticeLineService arrivalNoticeLineService,
                                    IStorageCoreService storageCoreService,
                                    FormLinkagePolicy formLinkagePolicy,
                                    MenuLinkageNoticeService menuLinkageNoticeService,
                                    ProductionNextStepLinkageService productionNextStepLinkageService,
                                    com.ktg.mes.linkage.IssueDraftAfterReceiptService issueDraftAfterReceiptService) {
        this.postingLockMapper = postingLockMapper;
        this.guardMapper = guardMapper;
        this.receiptService = receiptService;
        this.receiptLineService = receiptLineService;
        this.receiptDetailService = receiptDetailService;
        this.arrivalNoticeService = arrivalNoticeService;
        this.arrivalNoticeLineService = arrivalNoticeLineService;
        this.storageCoreService = storageCoreService;
        this.formLinkagePolicy = formLinkagePolicy;
        this.menuLinkageNoticeService = menuLinkageNoticeService;
        this.productionNextStepLinkageService = productionNextStepLinkageService;
        this.issueDraftAfterReceiptService = issueDraftAfterReceiptService;
    }

    /** 编制确认：只允许草稿 → 已确认，且必须已有入库行。 */
    @Transactional
    public void confirm(Long recptId, String operator) {
        if (recptId == null) {
            throw new ServiceException("请先保存入库单");
        }
        String persisted = postingLockMapper.itemRecpt(recptId);
        if (persisted == null) {
            throw new ServiceException("入库单不存在");
        }
        if (!UserConstants.ORDER_STATUS_PREPARE.equals(persisted)) {
            throw new ServiceException("只有草稿状态的入库单可以确认编制，当前状态：" + persisted);
        }
        if (linesOf(recptId).isEmpty()) {
            throw new ServiceException("请添加入库单行");
        }
        int rows = guardMapper.transitionReceiptStatus(recptId,
                UserConstants.ORDER_STATUS_PREPARE, UserConstants.ORDER_STATUS_CONFIRMED, operator);
        if (rows != 1) {
            throw new ServiceException("入库单状态已变更，请刷新后重试");
        }
    }

    /**
     * 普通编辑：在行锁保护的旧状态基础上校验目标状态是否合法。
     *
     * @param requireVendor 移动端要求必须选择供应商
     */
    @Transactional
    public void edit(WmItemRecpt request, String operator, boolean requireVendor) {
        if (request == null || request.getRecptId() == null) {
            throw new ServiceException("请先保存入库单");
        }
        Long recptId = request.getRecptId();
        String persistedStatus = postingLockMapper.itemRecpt(recptId);
        if (persistedStatus == null) {
            throw new ServiceException("入库单不存在");
        }
        WmItemRecpt persisted = receiptService.selectWmItemRecptByRecptId(recptId);
        if (persisted == null) {
            throw new ServiceException("入库单不存在");
        }

        String target = request.getStatus() == null ? persistedStatus : request.getStatus();
        if (UserConstants.ORDER_STATUS_FINISHED.equals(target)
                || UserConstants.ORDER_STATUS_CANCELED.equals(persistedStatus)
                || UserConstants.ORDER_STATUS_FINISHED.equals(persistedStatus)) {
            throw new ServiceException("已执行或已取消的入库单不能通过编辑修改，完成状态只能由执行操作产生");
        }
        Set<String> allowed = EDIT_TRANSITIONS.get(persistedStatus);
        if (allowed == null || !allowed.contains(target)) {
            throw new ServiceException("不允许把采购入库单从 " + persistedStatus + " 改为 " + target);
        }
        if (requireVendor && StringUtils.isNull(request.getVendorId())) {
            throw new ServiceException("请选择供应商！");
        }
        // 来源快照：已生成的来源单号不允许通过普通编辑改指到别的到货单。
        if (persisted.getNoticeId() != null && request.getNoticeId() != null
                && !persisted.getNoticeId().equals(request.getNoticeId())) {
            throw new ServiceException("入库单的来源到货通知不允许修改，如需更换请撤销后重新建单");
        }
        if (UserConstants.ORDER_STATUS_APPROVED.equals(persistedStatus)
                && persisted.getVendorId() != null
                && request.getVendorId() != null
                && !persisted.getVendorId().equals(request.getVendorId())) {
            throw new ServiceException("已提交待执行的入库单不允许更换供应商");
        }

        if (UserConstants.ORDER_STATUS_APPROVING.equals(target)) {
            if (linesOf(recptId).isEmpty()) {
                throw new ServiceException("请添加入库单行");
            }
        }
        if (UserConstants.ORDER_STATUS_APPROVED.equals(target)) {
            assertDetailsMatchLines(recptId);
        }

        if (!target.equals(persistedStatus)) {
            int rows = guardMapper.transitionReceiptStatus(recptId, persistedStatus, target, operator);
            if (rows != 1) {
                throw new ServiceException("入库单状态已变更，请刷新后重试");
            }
        }
        request.setStatus(target);
        request.setUpdateBy(operator);
        receiptService.updateWmItemRecpt(request);

        if (UserConstants.ORDER_STATUS_APPROVED.equals(target)) {
            try {
                menuLinkageNoticeService.markUnread(MenuLinkageNoticePaths.ITEM_RECPT);
            } catch (Exception ignore) {
                // 提醒失败不影响入库业务本身
            }
        }
    }

    /**
     * 执行入库：状态、来源、数量、检验结果全部在锁内校验；过账与状态更新同事务。
     */
    @Transactional
    public void execute(Long recptId, String operator) {
        if (recptId == null) {
            throw new ServiceException("请先保存入库单");
        }
        String persistedStatus = postingLockMapper.itemRecpt(recptId);
        if (!UserConstants.ORDER_STATUS_APPROVED.equals(persistedStatus)) {
            throw new ServiceException("单据不存在、已执行或尚未到可执行状态，请刷新后重试");
        }
        WmItemRecpt recpt = receiptService.selectWmItemRecptByRecptId(recptId);
        if (recpt == null) {
            throw new ServiceException("入库单不存在");
        }

        List<WmItemRecptLine> lines = linesOf(recptId);
        if (lines.isEmpty()) {
            throw new ServiceException("入库单没有明细行，不能执行入库");
        }
        // 来源状态与检验放行：只校验「本入库单实际引用的来源行」。
        // 修复前对整张到货通知校验，只要通知里存在一条全 NG（合格量 0）的需检行，
        // 就会把只含合格行的入库单整单拒绝，导致“合格行 + 全 NG 行”无法入库。
        WmArrivalNotice notice = recpt.getNoticeId() == null ? null
                : arrivalNoticeService.selectWmArrivalNoticeByNoticeId(recpt.getNoticeId());
        List<WmArrivalNoticeLine> noticeLines = notice == null
                ? Collections.<WmArrivalNoticeLine>emptyList() : linesOfNotice(notice.getNoticeId());
        Set<Long> referencedNoticeLineIds = new HashSet<>();
        for (WmItemRecptLine line : lines) {
            if (line.getNoticeLineId() != null) {
                referencedNoticeLineIds.add(line.getNoticeLineId());
            }
        }
        List<WmArrivalNoticeLine> sourceLines = new ArrayList<>();
        for (WmArrivalNoticeLine sourceLine : noticeLines) {
            if (referencedNoticeLineIds.contains(sourceLine.getLineId())) {
                sourceLines.add(sourceLine);
            }
        }
        formLinkagePolicy.assertPurchaseReceiptExecutable(recpt, sourceLines);

        // 以数据库中的来源行、检验单为准核对每行数量
        assertQuantitiesTrusted(recpt, lines, sourceLines);

        // 上架明细必须存在且与入库行数量一致
        assertDetailsMatchLines(recptId);
        List<ItemRecptTxBean> beans = receiptService.getTxBeans(recptId);
        assertBeansCoverLines(lines, beans);

        // 过账（库存流水 + 库存余额）
        storageCoreService.processItemRecpt(beans);

        int rows = guardMapper.transitionReceiptStatus(recptId,
                UserConstants.ORDER_STATUS_APPROVED, UserConstants.ORDER_STATUS_FINISHED, operator);
        if (rows != 1) {
            throw new ServiceException("入库单已被其他请求执行，请刷新后重试");
        }

        refreshArrivalNoticeStatus(recpt.getNoticeId(), operator);

        try {
            menuLinkageNoticeService.markRead(MenuLinkageNoticePaths.ITEM_RECPT);
        } catch (Exception ignore) {
            // 提醒失败不影响已完成的入库
        }
        // 领料草稿必须等入库事务提交后再尝试：否则其内部异常会把外层事务标记为
        // rollback-only（被 catch 后提交即 UnexpectedRollbackException），或让已正确的
        // 入库过账被一起回滚。失败只记录、可在排产/领料页重试。
        scheduleIssueDraftAfterCommit(recptId, operator);
    }

    private void scheduleIssueDraftAfterCommit(final Long recptId, final String operator) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runIssueDraft(recptId, operator);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runIssueDraft(recptId, operator);
            }
        });
    }

    private void runIssueDraft(Long recptId, String operator) {
        // 跨 Bean 调用，确保 REQUIRES_NEW 经代理生效（同类内部调用会让事务注解失效）
        issueDraftAfterReceiptService.prepareAfterReceipt(recptId, operator);
    }

    /** 删除：只有草稿可删，且先删除行。 */
    @Transactional
    public void remove(Long[] recptIds, String operator) {
        if (recptIds == null || recptIds.length == 0) {
            throw new ServiceException("请选择要删除的入库单");
        }
        for (Long id : recptIds) {
            String persisted = postingLockMapper.itemRecpt(id);
            if (persisted == null) {
                throw new ServiceException("入库单不存在");
            }
            if (!UserConstants.ORDER_STATUS_PREPARE.equals(persisted)) {
                throw new ServiceException("只能删除草稿状态的单据!");
            }
        }
        for (Long id : recptIds) {
            receiptLineService.deleteByRecptId(id);
        }
        receiptService.deleteWmItemRecptByRecptIds(recptIds);
    }

    /** 入库行只能在草稿状态维护；提交后冻结，避免伪造来源与数量。 */
    @Transactional
    public void assertLineMutable(Long recptId) {
        if (recptId == null) {
            throw new ServiceException("入库单不存在");
        }
        String persisted = postingLockMapper.itemRecpt(recptId);
        if (persisted == null) {
            throw new ServiceException("入库单不存在");
        }
        if (!UserConstants.ORDER_STATUS_PREPARE.equals(persisted)) {
            throw new ServiceException("只有草稿状态的入库单可以维护入库行，提交后请撤销再修改");
        }
    }

    /**
     * 入库行必须来自对应到货行：物料、检验要求、检验单和数量都以来源行为准，
     * 前端传入的 iqcId/iqcCheck 不能覆盖来源。
     */
    public WmArrivalNoticeLine bindSourceLine(WmItemRecptLine line) {
        if (line == null || line.getRecptId() == null) {
            throw new ServiceException("请先选择入库单");
        }
        WmItemRecpt recpt = receiptService.selectWmItemRecptByRecptId(line.getRecptId());
        if (recpt == null) {
            throw new ServiceException("入库单不存在");
        }
        if (line.getNoticeLineId() == null) {
            return null;
        }
        WmArrivalNoticeLine source = arrivalNoticeLineService.selectWmArrivalNoticeLineByLineId(line.getNoticeLineId());
        if (source == null) {
            throw new ServiceException("来源到货行不存在");
        }
        if (recpt.getNoticeId() != null && !recpt.getNoticeId().equals(source.getNoticeId())) {
            throw new ServiceException("所选到货行不属于当前入库单的到货通知");
        }
        if (line.getItemId() != null && !line.getItemId().equals(source.getItemId())) {
            throw new ServiceException("入库行物料必须与来源到货行一致");
        }
        line.setItemId(source.getItemId());
        line.setItemCode(source.getItemCode());
        line.setItemName(source.getItemName());
        line.setSpecification(source.getSpecification());
        line.setUnitOfMeasure(source.getUnitOfMeasure());
        line.setUnitName(source.getUnitName());
        line.setIqcCheck(source.getIqcCheck());
        line.setIqcId(source.getIqcId());
        line.setIqcCode(source.getIqcCode());
        return source;
    }

    /**
     * 入库行数量不得超过来源到货行的可入库数量（需检行以合格量为准，检验未完成时先按到货量控制草稿）。
     * PC 与移动端共用，避免两端各自实现导致口径漂移。
     */
    public void assertLineQuantityWithinSource(WmItemRecptLine line,
                                               WmArrivalNoticeLine source,
                                               Long excludeLineId) {
        if (source == null || line == null || line.getQuantityRecived() == null) {
            return;
        }
        BigDecimal processable = UserConstants.YES.equals(source.getIqcCheck())
                ? (source.getQuantityQuanlified() != null ? source.getQuantityQuanlified() : source.getQuantityArrival())
                : source.getQuantityArrival();
        if (processable == null) {
            return;
        }
        BigDecimal occupied = guardMapper.occupiedQuantityOnNoticeLine(source.getLineId(), excludeLineId);
        if (occupied == null) {
            occupied = ZERO;
        }
        if (occupied.add(line.getQuantityRecived()).compareTo(processable) > 0) {
            throw new ServiceException("入库数量超过来源到货行可入库数量：已占用 "
                    + occupied.stripTrailingZeros().toPlainString() + "，可入库 "
                    + processable.stripTrailingZeros().toPlainString());
        }
    }

    private void assertQuantitiesTrusted(WmItemRecpt recpt,
                                         List<WmItemRecptLine> lines,
                                         List<WmArrivalNoticeLine> sourceLines) {
        Map<Long, WmArrivalNoticeLine> sourceByLine = new HashMap<>();
        for (WmArrivalNoticeLine sourceLine : sourceLines) {
            sourceByLine.put(sourceLine.getLineId(), sourceLine);
        }
        for (WmItemRecptLine line : lines) {
            if (line.getQuantityRecived() == null || line.getQuantityRecived().compareTo(ZERO) <= 0) {
                throw new ServiceException("入库行数量必须大于0");
            }
            if (line.getNoticeLineId() == null) {
                // 历史数据/手工入库：没有来源行的需检行必须能指到一张已完成的检验单。
                if (UserConstants.YES.equals(line.getIqcCheck())) {
                    if (line.getIqcId() == null || guardMapper.countCompletedIqc(line.getIqcId()) != 1) {
                        throw new ServiceException("需检入库行缺少已完成的来料检验单，不能执行入库");
                    }
                }
                continue;
            }
            WmArrivalNoticeLine source = sourceByLine.get(line.getNoticeLineId());
            if (source == null) {
                throw new ServiceException("入库行对应的到货行不存在或不属于该到货通知");
            }
            if (!source.getItemId().equals(line.getItemId())) {
                throw new ServiceException("入库行物料与来源到货行不一致");
            }
            BigDecimal processable;
            if (UserConstants.YES.equals(source.getIqcCheck())) {
                if (source.getIqcId() == null) {
                    throw new ServiceException("来源到货行尚未完成来料检验，不能执行入库");
                }
                if (line.getIqcId() != null && !line.getIqcId().equals(source.getIqcId())) {
                    throw new ServiceException("入库行关联的检验单与来源到货行不一致");
                }
                if (guardMapper.countCompletedIqcBoundToArrivalLine(source.getIqcId(),
                        source.getNoticeId(), source.getLineId()) != 1) {
                    throw new ServiceException("来料检验单未完成或与来源行不匹配，不能执行入库");
                }
                processable = source.getQuantityQuanlified() == null ? ZERO : source.getQuantityQuanlified();
                if (processable.compareTo(ZERO) <= 0) {
                    throw new ServiceException("来源行合格数量为0，不能执行入库（NG 物料不得入库）");
                }
            } else {
                processable = source.getQuantityArrival() == null ? ZERO : source.getQuantityArrival();
                if (processable.compareTo(ZERO) <= 0) {
                    throw new ServiceException("来源行到货数量为0，不能执行入库");
                }
            }
            BigDecimal alreadyIn = guardMapper.finishedQuantityOnNoticeLine(source.getLineId(), recpt.getRecptId());
            BigDecimal total = alreadyIn.add(line.getQuantityRecived());
            if (total.compareTo(processable) > 0) {
                throw new ServiceException("入库数量超过来源可入库数量：已入库 "
                        + alreadyIn.stripTrailingZeros().toPlainString() + "，可入库 "
                        + processable.stripTrailingZeros().toPlainString() + "，本次 "
                        + line.getQuantityRecived().stripTrailingZeros().toPlainString());
            }
        }
    }

    private void assertDetailsMatchLines(Long recptId) {
        List<WmItemRecptLine> lines = linesOf(recptId);
        if (lines.isEmpty()) {
            throw new ServiceException("请添加入库单行");
        }
        StringBuilder message = new StringBuilder();
        for (WmItemRecptLine line : lines) {
            if (!UserConstants.EQUAL_TO.equals(receiptDetailService.checkQuantity(line.getLineId()))) {
                message.append(line.getItemName()).append(line.getBatchCode()).append("未完成上架！");
            }
        }
        if (message.length() > 0) {
            throw new ServiceException(message.toString());
        }
    }

    private void assertBeansCoverLines(List<WmItemRecptLine> lines, List<ItemRecptTxBean> beans) {
        if (beans == null || beans.isEmpty()) {
            throw new ServiceException("没有可过账的上架明细，不能执行入库");
        }
        Map<Long, BigDecimal> quantityByLine = new HashMap<>();
        for (ItemRecptTxBean bean : beans) {
            if (bean.getSourceDocLineId() == null) {
                continue;
            }
            BigDecimal current = quantityByLine.get(bean.getSourceDocLineId());
            BigDecimal quantity = bean.getTransactionQuantity() == null ? ZERO : bean.getTransactionQuantity();
            quantityByLine.put(bean.getSourceDocLineId(), current == null ? quantity : current.add(quantity));
        }
        for (WmItemRecptLine line : lines) {
            BigDecimal posted = quantityByLine.get(line.getLineId());
            if (posted == null || posted.compareTo(ZERO) <= 0) {
                throw new ServiceException("入库行[" + line.getItemName() + "]没有上架明细，不能执行入库");
            }
            if (posted.compareTo(line.getQuantityRecived()) != 0) {
                throw new ServiceException("入库行[" + line.getItemName() + "]上架数量与入库数量不一致");
            }
        }
    }

    /**
     * 到货通知状态按“每行可处理量 / 已入库累计量”汇总：
     * 只有全部行都收满才完成；部分入库只进入已放行（APPROVED）状态。
     */
    private void refreshArrivalNoticeStatus(Long noticeId, String operator) {
        if (noticeId == null) {
            return;
        }
        postingLockMapper.arrivalNotice(noticeId);
        WmArrivalNotice notice = arrivalNoticeService.selectWmArrivalNoticeByNoticeId(noticeId);
        if (notice == null) {
            return;
        }
        List<WmArrivalNoticeLine> lines = linesOfNotice(noticeId);
        BigDecimal processable = ZERO;
        boolean allInspected = true;
        for (WmArrivalNoticeLine line : lines) {
            if (UserConstants.YES.equals(line.getIqcCheck())) {
                if (line.getIqcId() == null || line.getQuantityQuanlified() == null) {
                    allInspected = false;
                    continue;
                }
                processable = processable.add(line.getQuantityQuanlified());
            } else {
                processable = processable.add(line.getQuantityArrival() == null ? ZERO : line.getQuantityArrival());
            }
        }
        BigDecimal received = guardMapper.finishedQuantityOnNotice(noticeId);
        String current = notice.getStatus();
        if (UserConstants.ORDER_STATUS_CANCELED.equals(current)) {
            return;
        }
        if (processable.compareTo(ZERO) > 0 && received.compareTo(processable) >= 0) {
            if (!UserConstants.ORDER_STATUS_FINISHED.equals(current)) {
                notice.setStatus(UserConstants.ORDER_STATUS_FINISHED);
                notice.setUpdateBy(operator);
                arrivalNoticeService.updateWmArrivalNotice(notice);
            }
        } else if (allInspected && !UserConstants.ORDER_STATUS_FINISHED.equals(current)) {
            notice.setStatus(UserConstants.ORDER_STATUS_APPROVED);
            notice.setUpdateBy(operator);
            arrivalNoticeService.updateWmArrivalNotice(notice);
        }
    }

    private List<WmItemRecptLine> linesOf(Long recptId) {
        WmItemRecptLine query = new WmItemRecptLine();
        query.setRecptId(recptId);
        List<WmItemRecptLine> lines = receiptLineService.selectWmItemRecptLineList(query);
        return lines == null ? new ArrayList<WmItemRecptLine>() : lines;
    }

    private List<WmArrivalNoticeLine> linesOfNotice(Long noticeId) {
        WmArrivalNoticeLine query = new WmArrivalNoticeLine();
        query.setNoticeId(noticeId);
        List<WmArrivalNoticeLine> lines = arrivalNoticeLineService.selectWmArrivalNoticeLineList(query);
        return lines == null ? new ArrayList<WmArrivalNoticeLine>() : lines;
    }
}
