package com.ktg.mes.wm.service.impl;

import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.DateUtils;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.mapper.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** 整单计算后一次保存；拣货只建立分配明细，出库继续走原有过账校验。 */
@Service
public class IssueAutoPickService {
    private final PostingLockMapper locks;
    private final WmIssueHeaderMapper headers;
    private final WmIssueLineMapper lines;
    private final WmIssueDetailMapper details;
    private final WmMaterialStockMapper stocks;

    public IssueAutoPickService(PostingLockMapper locks, WmIssueHeaderMapper headers,
            WmIssueLineMapper lines, WmIssueDetailMapper details, WmMaterialStockMapper stocks) {
        this.locks = locks; this.headers = headers; this.lines = lines; this.details = details; this.stocks = stocks;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pick(Long issueId, String operator) {
        if (issueId == null) throw new ServiceException("请先保存领料单");
        String status = locks.issue(issueId);
        if (!Arrays.asList("PREPARE", "APPROVING", "APPROVED").contains(status))
            throw new ServiceException("领料单不存在、已执行或已取消，请刷新后重试");
        // 与库存过账采用相同锁顺序，序列化本次计算和明细保存。
        locks.inventory();
        if (headers.selectWmIssueHeaderByIssueId(issueId) == null) throw new ServiceException("领料单不存在，请刷新后重试");
        WmIssueLine lineQuery = new WmIssueLine(); lineQuery.setIssueId(issueId);
        List<WmIssueLine> demand = new ArrayList<>(lines.selectWmIssueLineList(lineQuery));
        if (demand.isEmpty()) throw new ServiceException("领料单没有物料明细，请先添加领料物料");
        // 有指定批次的行先分配，避免普通行占用它唯一可用的批次。
        demand.sort(Comparator.comparing((WmIssueLine l) -> l.getBatchId() == null && blank(l.getBatchCode()))
                .thenComparing(WmIssueLine::getLineId));
        Map<Long, WmIssueLine> lineById = new HashMap<>();
        Map<Long, List<WmMaterialStock>> byItem = new HashMap<>();
        Map<Long, WmMaterialStock> byStock = new HashMap<>();
        Map<Long, BigDecimal> available = new HashMap<>();
        for (WmIssueLine line : demand) {
            if (line.getItemId() == null || line.getQuantityIssued() == null || line.getQuantityIssued().signum() <= 0)
                throw new ServiceException("物料【" + label(line) + "】领料数量必须大于 0");
            lineById.put(line.getLineId(), line);
            if (!byItem.containsKey(line.getItemId())) {
                List<WmMaterialStock> candidates = stocks.selectIssuePickStocks(line.getItemId());
                List<WmMaterialStock> eligible = (candidates == null ? Collections.<WmMaterialStock>emptyList() : candidates)
                        .stream().filter(s -> usable(s) && Objects.equals(s.getItemId(), line.getItemId()))
                        .sorted(Comparator.comparing(WmMaterialStock::getRecptDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(WmMaterialStock::getMaterialStockId)).collect(Collectors.toList());
                byItem.put(line.getItemId(), eligible);
                for (WmMaterialStock stock : eligible) {
                    byStock.put(stock.getMaterialStockId(), stock);
                    available.put(stock.getMaterialStockId(), stock.getQuantityOnhand());
                }
            }
        }
        WmIssueDetail detailQuery = new WmIssueDetail(); detailQuery.setIssueId(issueId);
        List<WmIssueDetail> existing = details.selectWmIssueDetailList(detailQuery);
        Map<Long, BigDecimal> pickedByLine = new HashMap<>();
        for (WmIssueDetail detail : existing) {
            WmIssueLine line = lineById.get(detail.getLineId());
            Long stockId;
            try { stockId = Long.valueOf(detail.getMaterialStockId()); }
            catch (RuntimeException e) { throw new ServiceException("已有拣货明细缺少有效库存，请先调整明细"); }
            WmMaterialStock stock = byStock.get(stockId);
            if (line == null || stock == null || !Objects.equals(detail.getIssueId(), issueId)
                    || !Objects.equals(detail.getItemId(), line.getItemId()) || !matches(stock, line)
                    || !sameStock(detail, stock) || detail.getQuantity() == null || detail.getQuantity().signum() <= 0)
                throw new ServiceException("已有拣货明细的物料、批次或库位不可用，请先调整明细");
            available.put(stockId, available.get(stockId).subtract(detail.getQuantity()));
            if (available.get(stockId).signum() < 0)
                throw new ServiceException("物料【" + label(line) + "】已有拣货数量超过现存量，请先调整明细");
            pickedByLine.merge(line.getLineId(), detail.getQuantity(), BigDecimal::add);
        }
        List<WmIssueDetail> plan = new ArrayList<>();
        List<String> shortages = new ArrayList<>();
        for (WmIssueLine line : demand) {
            BigDecimal remaining = line.getQuantityIssued().subtract(pickedByLine.getOrDefault(line.getLineId(), BigDecimal.ZERO));
            if (remaining.signum() < 0) throw new ServiceException("物料【" + label(line) + "】已超拣，请先调整明细");
            for (WmMaterialStock stock : byItem.get(line.getItemId())) {
                if (remaining.signum() == 0) break;
                if (!matches(stock, line)) continue;
                Long stockId = stock.getMaterialStockId();
                BigDecimal take = remaining.min(available.get(stockId));
                if (take.signum() <= 0) continue;
                WmIssueDetail detail = new WmIssueDetail();
                BeanUtils.copyProperties(stock, detail, "materialStockId", "createBy", "createTime", "updateBy", "updateTime", "params");
                detail.setIssueId(issueId); detail.setLineId(line.getLineId());
                detail.setItemCode(line.getItemCode()); detail.setItemName(line.getItemName());
                detail.setSpecification(line.getSpecification()); detail.setUnitOfMeasure(line.getUnitOfMeasure());
                detail.setUnitName(line.getUnitName()); detail.setMaterialStockId(String.valueOf(stockId));
                detail.setQuantity(take); detail.setCreateBy(operator); detail.setCreateTime(DateUtils.getNowDate());
                plan.add(detail); remaining = remaining.subtract(take);
                available.put(stockId, available.get(stockId).subtract(take));
            }
            if (remaining.signum() > 0)
                shortages.add("【" + label(line) + "】尚缺 " + remaining.stripTrailingZeros().toPlainString() + safe(line.getUnitName()));
        }
        if (!shortages.isEmpty()) throw new ServiceException("可用库存不足：" + String.join("；", shortages) + "。本单未新增拣货，请补料或调整后重试");
        for (WmIssueDetail detail : plan) {
            if (details.insertWmIssueDetail(detail) != 1) throw new ServiceException("拣货保存失败，请刷新后重试");
        }
        if (!"APPROVED".equals(status)) {
            WmIssueHeader update = new WmIssueHeader(); update.setIssueId(issueId); update.setStatus("APPROVED");
            update.setUpdateBy(operator); update.setUpdateTime(DateUtils.getNowDate());
            if (headers.updateWmIssueHeader(update) != 1) throw new ServiceException("领料单状态更新失败，请刷新后重试");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "APPROVED"); result.put("lineCount", demand.size());
        result.put("detailCount", plan.size()); result.put("existingDetailCount", existing.size());
        return result;
    }

    private boolean usable(WmMaterialStock s) {
        return s.getMaterialStockId() != null && s.getQuantityOnhand() != null && s.getQuantityOnhand().signum() > 0
                && !"Y".equals(s.getFrozenFlag()) && !safe(s.getWarehouseCode()).toUpperCase(Locale.ROOT).contains("VIR")
                && s.getBatchId() != null && s.getWarehouseId() != null && s.getLocationId() != null && s.getAreaId() != null;
    }
    private boolean matches(WmMaterialStock s, WmIssueLine l) {
        return (l.getBatchId() == null || Objects.equals(l.getBatchId(), s.getBatchId()))
                && (blank(l.getBatchCode()) || Objects.equals(l.getBatchCode(), s.getBatchCode()));
    }
    private boolean sameStock(WmIssueDetail d, WmMaterialStock s) {
        return Objects.equals(d.getBatchId(), s.getBatchId()) && Objects.equals(d.getBatchCode(), s.getBatchCode())
                && Objects.equals(d.getWarehouseId(), s.getWarehouseId()) && Objects.equals(d.getWarehouseCode(), s.getWarehouseCode())
                && Objects.equals(d.getLocationId(), s.getLocationId()) && Objects.equals(d.getLocationCode(), s.getLocationCode())
                && Objects.equals(d.getAreaId(), s.getAreaId()) && Objects.equals(d.getAreaCode(), s.getAreaCode());
    }
    private String label(WmIssueLine l) { return blank(l.getItemCode()) ? String.valueOf(l.getItemId()) : l.getItemCode(); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String safe(String value) { return value == null ? "" : value; }
}
