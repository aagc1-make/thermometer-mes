package com.ktg.mes.wm.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.domain.WmProductRecpt;
import com.ktg.mes.wm.domain.WmProductRecptDetail;
import com.ktg.mes.wm.domain.WmProductRecptLine;
import com.ktg.mes.wm.mapper.ProductReceiptGuardMapper;
import com.ktg.mes.wm.service.IWmProductRecptDetailService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品入库过账前的来源/数量校验（PC 与移动端共用）。
 *
 * <p>口径（临时，待业务确认）：来源 = 工单最后产出工序的合格产出按批次汇总；
 * 放行证据 = 终末工序(包装)存在 FINISHED 报工；累计入库 ≤ min(合格产出合计, 有效包装量)。</p>
 *
 * <p>抽成独立 Service 的原因：此前只有 PC 控制器执行该校验，移动端 execute 可直接过账，
 * 同一业务动作在两端规则不一致。</p>
 */
@Service
public class ProductReceiptGuardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ProductReceiptGuardMapper guardMapper;
    private final IWmProductRecptDetailService detailService;

    public ProductReceiptGuardService(ProductReceiptGuardMapper guardMapper,
                                      IWmProductRecptDetailService detailService) {
        this.guardMapper = guardMapper;
        this.detailService = detailService;
    }

    /**
     * 按来源校验：来源工序可确定则强制校验，不确定则拒绝（不跳过）。
     * 校验项：物料一致、上架明细物料/批次/库位、按工单+批次汇总的累计上限、包装放行上限。
     */
    public void assertWithinSource(WmProductRecpt recpt, List<WmProductRecptLine> lines) {
        // 放行证据：终末工序(包装)必须有 FINISHED 报工
        Long terminalProcessId = guardMapper.terminalProcessId(recpt.getItemId());
        if (terminalProcessId == null
                || guardMapper.countFinishedFeedbackAtProcess(recpt.getWorkorderId(), terminalProcessId) == 0) {
            throw new ServiceException("产品入库放行条件未满足（包装工序未实际完成），禁止过账");
        }
        // 产出来源工序应恰好一道；无或多道视为口径不确定，拒绝
        List<Long> processes = guardMapper.produceProcessesForProduct(recpt.getWorkorderId(), recpt.getItemId());
        if (processes == null || processes.size() != 1) {
            throw new ServiceException("产品入库单来源工序或合格产出无法确定，禁止过账，请转人工处理");
        }
        Long processId = processes.get(0);
        // 多批次来源阻断：包装报工无批次，无法按批次可靠分配包装量
        List<Map<String, Object>> sourceBatches = guardMapper.qualifiedOutputByBatch(
                recpt.getWorkorderId(), recpt.getItemId(), processId);
        if (sourceBatches != null && sourceBatches.size() > 1) {
            throw new ServiceException("产品入库来源存在多批次合格产出，包装报工无批次无法可靠关联，请转人工处理");
        }
        // 按批次汇总本单所有行，逐批校验累计上限
        Map<String, BigDecimal> byBatch = new HashMap<>();
        for (WmProductRecptLine line : lines) {
            if (line.getItemId() != null && !line.getItemId().equals(recpt.getItemId())) {
                throw new ServiceException("产品入库行物料与入库单产品不一致");
            }
            if (!UserConstants.EQUAL_TO.equals(detailService.checkQuantity(line.getLineId()))) {
                throw new ServiceException("产品入库行[" + line.getItemName() + "]上架明细数量与行数量不一致");
            }
            assertDetailsConsistent(line);
            String batch = line.getBatchCode() == null ? "" : line.getBatchCode();
            BigDecimal qty = line.getQuantityRecived() == null ? ZERO : line.getQuantityRecived();
            BigDecimal current = byBatch.get(batch);
            byBatch.put(batch, (current == null ? ZERO : current).add(qty));
        }
        for (Map.Entry<String, BigDecimal> entry : byBatch.entrySet()) {
            String batch = entry.getKey();
            BigDecimal produced = guardMapper.qualifiedOutputForBatch(
                    recpt.getWorkorderId(), recpt.getItemId(), batch, processId);
            BigDecimal finished = guardMapper.finishedProductRecptByBatch(recpt.getWorkorderId(), batch);
            if (produced == null) {
                produced = ZERO;
            }
            if (finished == null) {
                finished = ZERO;
            }
            BigDecimal total = finished.add(entry.getValue());
            if (total.compareTo(produced) > 0) {
                throw new ServiceException("产品入库数量超过来源工序合格产出：已入库 "
                        + finished.stripTrailingZeros().toPlainString() + "，来源合格 "
                        + produced.stripTrailingZeros().toPlainString() + "，本单 "
                        + entry.getValue().stripTrailingZeros().toPlainString() + "，批次 "
                        + (batch.isEmpty() ? "(空)" : batch));
            }
        }
        // 工单级包装放行上限：累计入库 ≤ min(合格产出合计, 有效完成包装量)
        BigDecimal totalOutput = guardMapper.totalQualifiedOutput(
                recpt.getWorkorderId(), recpt.getItemId(), processId);
        BigDecimal packagingCap = guardMapper.completedPackagingQuantity(
                recpt.getWorkorderId(), terminalProcessId);
        if (totalOutput == null) {
            totalOutput = ZERO;
        }
        if (packagingCap == null) {
            packagingCap = totalOutput;
        }
        BigDecimal cap = totalOutput.min(packagingCap);
        BigDecimal totalFinished = guardMapper.finishedProductRecptTotal(recpt.getWorkorderId());
        if (totalFinished == null) {
            totalFinished = ZERO;
        }
        BigDecimal totalThis = ZERO;
        for (BigDecimal v : byBatch.values()) {
            totalThis = totalThis.add(v);
        }
        if (totalFinished.add(totalThis).compareTo(cap) > 0) {
            throw new ServiceException("产品入库数量超过有效完成包装量：已入库 "
                    + totalFinished.stripTrailingZeros().toPlainString() + "，有效包装 "
                    + cap.stripTrailingZeros().toPlainString() + "，本单 "
                    + totalThis.stripTrailingZeros().toPlainString());
        }
    }

    /** 每条上架明细的物料、批次、库位必须与来源行一致（库位必填）。 */
    public void assertDetailsConsistent(WmProductRecptLine line) {
        WmProductRecptDetail query = new WmProductRecptDetail();
        query.setLineId(line.getLineId());
        List<WmProductRecptDetail> details = detailService.selectWmProductRecptDetailList(query);
        if (details == null || details.isEmpty()) {
            throw new ServiceException("产品入库行[" + line.getItemName() + "]缺少上架明细");
        }
        for (WmProductRecptDetail detail : details) {
            if (detail.getItemId() != null && !detail.getItemId().equals(line.getItemId())) {
                throw new ServiceException("上架明细物料与产品入库行不一致");
            }
            if (detail.getBatchCode() != null && line.getBatchCode() != null
                    && !detail.getBatchCode().equals(line.getBatchCode())) {
                throw new ServiceException("上架明细批次与产品入库行不一致");
            }
            if (detail.getWarehouseId() == null || detail.getLocationId() == null || detail.getAreaId() == null) {
                throw new ServiceException("上架明细库位不完整，请分配仓库/库区/库位");
            }
        }
    }
}
