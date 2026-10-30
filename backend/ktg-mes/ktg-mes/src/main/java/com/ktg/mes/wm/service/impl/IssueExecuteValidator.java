package com.ktg.mes.wm.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ktg.mes.wm.domain.WmIssueDetail;
import com.ktg.mes.wm.domain.WmIssueLine;
import com.ktg.mes.wm.domain.WmMaterialStock;
import com.ktg.mes.wm.mapper.WmMaterialStockMapper;
import com.ktg.mes.wm.service.IWmIssueDetailService;

/**
 * 生产领料执行前的逐行校验（PC 与移动端共用）。
 *
 * 规则：
 * 1) 每行必须有拣货明细；
 * 2) 每条明细数量 > 0，且该行明细合计 = 领料数量；
 * 3) 明细物料必须与领料行一致；
 * 4) 明细对应库存行的现存量必须足够（按 物料+批次+仓库+库区+库位 汇总）。
 *
 * 返回 null 表示通过；否则返回面向用户的业务提示（含物料与差异数量），调用方直接作为 AjaxResult.error 返回，
 * 不写库存、不写流水、不改单据状态。
 */
@Service
public class IssueExecuteValidator {

    @Autowired
    private IWmIssueDetailService wmIssueDetailService;

    @Autowired
    private WmMaterialStockMapper wmMaterialStockMapper;

    public String validate(List<WmIssueLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "请指定领出的物资";
        }
        Map<Long, BigDecimal> totalByStock = new TreeMap<Long, BigDecimal>();
        Map<Long, WmIssueDetail> referenceByStock = new HashMap<Long, WmIssueDetail>();
        for (WmIssueLine line : lines) {
            String itemCode = line.getItemCode() == null ? String.valueOf(line.getItemId()) : line.getItemCode();
            WmIssueDetail param = new WmIssueDetail();
            param.setIssueId(line.getIssueId());
            param.setLineId(line.getLineId());
            List<WmIssueDetail> details = wmIssueDetailService.selectWmIssueDetailList(param);
            if (details == null || details.isEmpty()) {
                return "物料【" + itemCode + "】尚未拣货，请先完成拣货明细再执行领出";
            }
            BigDecimal quantityIssued = line.getQuantityIssued() == null ? BigDecimal.ZERO : line.getQuantityIssued();
            BigDecimal sum = BigDecimal.ZERO;
            for (WmIssueDetail d : details) {
                BigDecimal q = d.getQuantity() == null ? BigDecimal.ZERO : d.getQuantity();
                if (q.compareTo(BigDecimal.ZERO) <= 0) {
                    return "物料【" + itemCode + "】存在数量不大于 0 的拣货明细，请修正后再执行";
                }
                if (line.getItemId() == null || !Objects.equals(d.getItemId(), line.getItemId())) {
                    return "物料【" + itemCode + "】的拣货明细物料与领料行不一致，请修正后再执行";
                }
                sum = sum.add(q);
                if (d.getMaterialStockId() == null || d.getMaterialStockId().trim().isEmpty()) {
                    return "物料【" + itemCode + "】拣货明细缺少库存行，请重新拣货";
                }
                final Long stockId;
                try { stockId = Long.valueOf(d.getMaterialStockId()); }
                catch (NumberFormatException e) { return "物料【" + itemCode + "】库存行无效，请重新拣货"; }
                totalByStock.put(stockId, totalByStock.containsKey(stockId) ? totalByStock.get(stockId).add(q) : q);
                WmIssueDetail prior = referenceByStock.putIfAbsent(stockId, d);
                if (prior != null && (!Objects.equals(prior.getItemId(), d.getItemId())
                        || !Objects.equals(prior.getBatchId(), d.getBatchId())
                        || !Objects.equals(prior.getBatchCode(), d.getBatchCode())
                        || !Objects.equals(prior.getWarehouseId(), d.getWarehouseId())
                        || !Objects.equals(prior.getWarehouseCode(), d.getWarehouseCode())
                        || !Objects.equals(prior.getLocationId(), d.getLocationId())
                        || !Objects.equals(prior.getLocationCode(), d.getLocationCode())
                        || !Objects.equals(prior.getAreaId(), d.getAreaId())
                        || !Objects.equals(prior.getAreaCode(), d.getAreaCode()))) {
                    return "物料【" + itemCode + "】同一库存行的拣货信息不一致";
                }
            }
            if (sum.compareTo(quantityIssued) != 0) {
                BigDecimal diff = quantityIssued.subtract(sum);
                return "物料【" + itemCode + "】拣货数量 " + strip(sum) + " 与领料数量 " + strip(quantityIssued)
                        + " 不一致" + (diff.compareTo(BigDecimal.ZERO) > 0 ? "，尚差 " + strip(diff) : "，超出 " + strip(diff.negate()))
                        + "，请调整拣货明细后再执行";
            }
        }
            // 按固定顺序锁库存并使用当前读，锁保持至过账事务结束。
            for (Map.Entry<Long, BigDecimal> entry : totalByStock.entrySet()) {
                Long stockId = entry.getKey();
                WmIssueDetail d = referenceByStock.get(stockId);
                String itemCode = safe(d.getItemCode());
                WmMaterialStock stock = wmMaterialStockMapper.lockMaterialStockById(stockId);
                if (stock == null) {
                    return "物料【" + itemCode + "】的拣货明细引用的库存行不存在，请重新拣货";
                }
                BigDecimal onhand = stock.getQuantityOnhand() == null ? BigDecimal.ZERO : stock.getQuantityOnhand();
                if (!Objects.equals(stock.getItemId(), d.getItemId())
                        || d.getBatchId() == null || !Objects.equals(stock.getBatchId(), d.getBatchId())
                        || !Objects.equals(stock.getBatchCode(), d.getBatchCode())
                        || d.getWarehouseId() == null || !Objects.equals(stock.getWarehouseId(), d.getWarehouseId())
                        || !Objects.equals(stock.getWarehouseCode(), d.getWarehouseCode())
                        || d.getLocationId() == null || !Objects.equals(stock.getLocationId(), d.getLocationId())
                        || !Objects.equals(stock.getLocationCode(), d.getLocationCode())
                        || d.getAreaId() == null || !Objects.equals(stock.getAreaId(), d.getAreaId())
                        || !Objects.equals(stock.getAreaCode(), d.getAreaCode())) {
                    return "物料【" + itemCode + "】拣货物料、批次或库位与库存不一致";
                }
                BigDecimal need = entry.getValue();
                if (need != null && onhand.compareTo(need) < 0) {
                    return "物料【" + itemCode + "】批次【" + safe(d.getBatchCode()) + "】库存不足：现存量 " + strip(onhand)
                            + "，本次拣货 " + strip(need) + "，请调整拣货数量";
                }
            }
        return null;
    }

    private String strip(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
