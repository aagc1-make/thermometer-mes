package com.ktg.mes.wm.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.wm.domain.WmProductRecpt;
import com.ktg.mes.wm.domain.WmProductRecptDetail;
import com.ktg.mes.wm.domain.WmProductRecptLine;
import com.ktg.mes.wm.domain.vo.StorageAreaOptionVo;
import com.ktg.mes.wm.domain.vo.StorageLocationOptionVo;
import com.ktg.mes.wm.mapper.ProductStockingMapper;
import com.ktg.mes.wm.service.IWmProductRecptDetailService;
import com.ktg.mes.wm.service.IWmProductRecptLineService;
import com.ktg.mes.wm.service.IWmProductRecptService;
import com.ktg.mes.wm.service.IWmProductStockingService;
import com.ktg.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品上架：库区/库位余量、推荐与混放校验实现。
 *
 * <p>余量口径：剩余 = 容量 - 在库数量(仅正库存) - 未过账上架预占。
 * 容量(NULL)表示不限量。</p>
 *
 * <p>混放口径（可配置）：
 * 1) 全局开关 mes.wm.mixing.enforce=false 时不做混放校验；
 * 2) 否则按库位 product_mixing / batch_mixing 开关判断；
 * 3) 若该库位已有“相同物料+相同批次”，允许继续入库（不因历史遗留的批次混放卡住正常业务）；
 * 4) 否则库位存在其它物料且未允许产品混放 → 拦截；存在同物料其它批次且未允许批次混放 → 拦截。</p>
 */
@Service
public class WmProductStockingServiceImpl implements IWmProductStockingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String CONFIG_MIXING_ENFORCE = "mes.wm.mixing.enforce";
    private static final String YES = "Y";

    @Autowired
    private ProductStockingMapper stockingMapper;

    @Autowired
    private IWmProductRecptService recptService;

    @Autowired
    private IWmProductRecptLineService lineService;

    @Autowired
    private IWmProductRecptDetailService detailService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private com.ktg.mes.wm.service.IWmStorageLocationService locationService;

    @Override
    public List<StorageLocationOptionVo> locationOptions(Long warehouseId, WmProductRecptLine line, BigDecimal requiredQty) {
        Long itemId = line.getItemId();
        String batchCode = line.getBatchCode();
        // 余量按库区计算；同一单据内的待上架明细全部计入预占
        List<StorageLocationOptionVo> rows = stockingMapper.selectLocationRows(
                warehouseId, itemId, line.getBatchId(), batchCode, null);
        BigDecimal required = requiredQty == null ? ZERO : requiredQty;
        for (StorageLocationOptionVo row : rows) {
            fillRemaining(row);
            List<StorageAreaOptionVo> areas = areaOptions(row.getLocationId(), line, required);
            row.setAreas(areas);
            // 库区级混放：只有当该库区下所有库位都被拦截时才视为库区不可用
            StorageAreaOptionVo blocked = areas.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getMixingBlocked()))
                    .findFirst().orElse(null);
            boolean allBlocked = !areas.isEmpty() && blocked != null
                    && areas.stream().allMatch(a -> Boolean.TRUE.equals(a.getMixingBlocked()));
            row.setMixingBlocked(allBlocked);
            row.setMixingMessage(allBlocked ? blocked.getMixingMessage() : null);
            row.setRecommended(false);
        }
        markLocationRecommendation(rows, required);
        rows.sort(Comparator
                .comparing((StorageLocationOptionVo r) -> !Boolean.TRUE.equals(r.getRecommended()))
                .thenComparing(r -> Boolean.TRUE.equals(r.getMixingBlocked()))
                .thenComparing(r -> remainingForSort(r.getRemaining()), Comparator.reverseOrder()));
        return rows;
    }

    @Override
    public List<StorageAreaOptionVo> areaOptions(Long locationId, WmProductRecptLine line, BigDecimal requiredQty) {
        List<StorageAreaOptionVo> rows = stockingMapper.selectAreaRows(
                locationId, line.getItemId(), line.getBatchId(), line.getBatchCode(), null);
        BigDecimal required = requiredQty == null ? ZERO : requiredQty;
        for (StorageAreaOptionVo row : rows) {
            fillAreaRemaining(row);
            checkAreaMixing(row, line);
        }
        markAreaRecommendation(rows, required, line);
        rows.sort(Comparator
                .comparing((StorageAreaOptionVo a) -> !Boolean.TRUE.equals(a.getRecommended()))
                .thenComparing(a -> Boolean.TRUE.equals(a.getMixingBlocked()))
                .thenComparing(a -> remainingForSort(a.getRemaining()), Comparator.reverseOrder()));
        return rows;
    }

    @Override
    public void assertDetailAllowed(WmProductRecptDetail detail) {
        if (detail == null || detail.getLineId() == null) {
            throw new ServiceException("上架明细缺少入库行");
        }
        WmProductRecptLine line = lineService.selectWmProductRecptLineByLineId(detail.getLineId());
        if (line == null) {
            throw new ServiceException("产品入库行不存在，无法上架");
        }
        WmProductRecpt recpt = recptService.selectWmProductRecptByRecptId(line.getRecptId());
        if (recpt != null) {
            String status = recpt.getStatus();
            if (UserConstants.ORDER_STATUS_PREPARE.equals(status)
                    || UserConstants.ORDER_STATUS_CONFIRMED.equals(status)) {
                throw new ServiceException("当前单据为" + ("PREPARE".equals(status) ? "草稿" : "已确认")
                        + "状态，请先提交到“待上架”后再上架");
            }
            if (UserConstants.ORDER_STATUS_FINISHED.equals(status)
                    || UserConstants.ORDER_STATUS_CANCELED.equals(status)) {
                throw new ServiceException("已入库或已取消的单据不允许再上架");
            }
        }
        if (detail.getQuantity() == null || detail.getQuantity().compareTo(ZERO) <= 0) {
            throw new ServiceException("上架数量必须大于0");
        }
        BigDecimal lineQty = line.getQuantityRecived() == null ? ZERO : line.getQuantityRecived();
        BigDecimal stocked = nullToZero(stockingMapper.selectStockedQtyByLine(detail.getLineId(), detail.getDetailId()));
        BigDecimal canStock = lineQty.subtract(stocked);
        if (canStock.compareTo(ZERO) <= 0) {
            throw new ServiceException("产品入库行[" + line.getItemName() + "]已上架数量已达入库数量 "
                    + plain(lineQty) + "，无需继续上架");
        }
        if (detail.getQuantity().compareTo(canStock) > 0) {
            throw new ServiceException("上架数量超过入库行可上架数量：本行入库 " + plain(lineQty)
                    + "，已上架 " + plain(stocked) + "，还差 " + plain(canStock)
                    + "，本次最多可上架 " + plain(canStock));
        }
        // 库位齐备
        if (detail.getLocationId() == null || detail.getAreaId() == null) {
            throw new ServiceException("请选择库区和库位");
        }
        List<StorageAreaOptionVo> areas = stockingMapper.selectAreaRows(
                detail.getLocationId(), line.getItemId(), detail.getBatchId(), detail.getBatchCode(), detail.getDetailId());
        StorageAreaOptionVo area = areas.stream()
                .filter(a -> a.getAreaId().equals(detail.getAreaId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("库位不存在、已冻结或已停用，请重新选择"));
        fillAreaRemaining(area);
        if (!area.getUnlimited() && area.getRemaining().compareTo(detail.getQuantity()) < 0) {
            throw new ServiceException("库位[" + area.getAreaName() + "]余量不足：容量 " + plain(area.getCapacity())
                    + "，已占用 " + plain(area.getOccupied()) + "，待上架预占 " + plain(area.getReserved())
                    + "，剩余 " + plain(area.getRemaining()) + "，本次需要 " + plain(detail.getQuantity()));
        }
        checkAreaMixing(area, line);
        if (Boolean.TRUE.equals(area.getMixingBlocked())) {
            throw new ServiceException(area.getMixingMessage()
                    + "；建议上架到 " + recommendAreaText(detail.getLocationId(), line, detail.getQuantity(), detail.getAreaId()));
        }
    }

    @Override
    public void assertReadyToStock(Long recptId) {
        WmProductRecpt recpt = recptService.selectWmProductRecptByRecptId(recptId);
        if (recpt == null) {
            throw new ServiceException("产品入库单不存在");
        }
        if (UserConstants.ORDER_STATUS_FINISHED.equals(recpt.getStatus())
                || UserConstants.ORDER_STATUS_CANCELED.equals(recpt.getStatus())) {
            throw new ServiceException("已入库或已取消的单据不能重复上架");
        }
        WmProductRecptLine query = new WmProductRecptLine();
        query.setRecptId(recptId);
        List<WmProductRecptLine> lines = lineService.selectWmProductRecptLineList(query);
        if (lines == null || lines.isEmpty()) {
            throw new ServiceException("请先添加入库行");
        }
        for (WmProductRecptLine line : lines) {
            BigDecimal lineQty = line.getQuantityRecived() == null ? ZERO : line.getQuantityRecived();
            BigDecimal stocked = nullToZero(stockingMapper.selectStockedQtyByLine(line.getLineId(), null));
            if (stocked.compareTo(lineQty) < 0) {
                throw new ServiceException("产品入库行[" + line.getItemName() + "]上架数量不足：入库 "
                        + plain(lineQty) + "，已上架 " + plain(stocked) + "，还差 " + plain(lineQty.subtract(stocked)));
            }
            if (stocked.compareTo(lineQty) > 0) {
                throw new ServiceException("产品入库行[" + line.getItemName() + "]上架数量超过入库数量：入库 "
                        + plain(lineQty) + "，已上架 " + plain(stocked) + "，请先减少上架明细");
            }
        }
        // 再按库位汇总复校验余量与混放，避免前端绕过
        WmProductRecptDetail detailQuery = new WmProductRecptDetail();
        detailQuery.setRecptId(recptId);
        List<WmProductRecptDetail> details = detailService.selectWmProductRecptDetailList(detailQuery);
        if (details == null || details.isEmpty()) {
            throw new ServiceException("请先完成上架明细");
        }
        // 同一库位的上架数量先汇总，再与“容量-占用-预占”比较（预占含本单明细）
        java.util.Map<Long, StorageAreaOptionVo> areaCache = new java.util.HashMap<>();
        for (WmProductRecptDetail detail : details) {
            WmProductRecptLine line = lineService.selectWmProductRecptLineByLineId(detail.getLineId());
            if (line == null) {
                throw new ServiceException("上架明细关联的入库行不存在");
            }
            if (detail.getLocationId() == null || detail.getAreaId() == null) {
                throw new ServiceException("上架明细库位不完整，请分配仓库/库区/库位");
            }
            StorageAreaOptionVo area = areaCache.get(detail.getAreaId());
            if (area == null) {
                List<StorageAreaOptionVo> areas = stockingMapper.selectAreaRows(
                        detail.getLocationId(), line.getItemId(), detail.getBatchId(), detail.getBatchCode(), null);
                area = areas.stream()
                        .filter(a -> a.getAreaId().equals(detail.getAreaId()))
                        .findFirst()
                        .orElseThrow(() -> new ServiceException("库位不存在、已冻结或已停用，请重新选择"));
                fillAreaRemaining(area);
                areaCache.put(detail.getAreaId(), area);
            }
            if (!area.getUnlimited() && area.getRemaining().compareTo(ZERO) < 0) {
                throw new ServiceException("库位[" + area.getAreaName() + "]余量不足：容量 " + plain(area.getCapacity())
                        + "，已占用 " + plain(area.getOccupied()) + "，本单及待上架预占 " + plain(area.getReserved()));
            }
            checkAreaMixing(area, line);
            if (Boolean.TRUE.equals(area.getMixingBlocked())) {
                throw new ServiceException(area.getMixingMessage()
                        + "；建议上架到 " + recommendAreaText(detail.getLocationId(), line, detail.getQuantity(), detail.getAreaId()));
            }
        }
    }

    @Override
    public BigDecimal remainingQty(WmProductRecptLine line) {
        if (line == null || line.getLineId() == null) {
            return ZERO;
        }
        BigDecimal lineQty = line.getQuantityRecived() == null ? ZERO : line.getQuantityRecived();
        BigDecimal stocked = nullToZero(stockingMapper.selectStockedQtyByLine(line.getLineId(), null));
        BigDecimal remain = lineQty.subtract(stocked);
        return remain.compareTo(ZERO) < 0 ? ZERO : remain;
    }

    // ------------------------------------------------------------------ 推荐

    /**
     * 推荐顺序：同物料同批次已存放库区 > 余量足够且最大 > 成品区兜底。
     */
    private void markLocationRecommendation(List<StorageLocationOptionVo> rows, BigDecimal required) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<StorageLocationOptionVo> usable = rows.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getMixingBlocked()))
                .filter(r -> r.getUnlimited() || r.getRemaining().compareTo(required) >= 0)
                .collect(Collectors.toList());
        StorageLocationOptionVo pick = null;
        String reason = null;
        // 1) 同物料同批次已存放
        pick = usable.stream()
                .filter(r -> nullToZero(r.getSameItemBatchQty()).compareTo(ZERO) > 0)
                .max(Comparator.comparing(r -> nullToZero(r.getSameItemBatchQty())))
                .orElse(null);
        if (pick != null) {
            reason = "该库区已存放同物料同批次 " + plain(pick.getSameItemBatchQty()) + "，优先归集";
        }
        // 2) 余量足够且最大
        if (pick == null) {
            pick = usable.stream()
                    .max(Comparator.comparing(r -> remainingForSort(r.getRemaining())))
                    .orElse(null);
            if (pick != null) {
                reason = "余量充足（" + (pick.getUnlimited() ? "不限" : plain(pick.getRemaining())) + "），可满足本次 "
                        + plain(required);
            }
        }
        // 3) 成品区兜底
        if (pick == null) {
            pick = rows.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getMixingBlocked()))
                    .filter(r -> "FG".equalsIgnoreCase(r.getWarehouseType()))
                    .findFirst().orElse(null);
            if (pick != null) {
                reason = "成品仓兜底库区";
            }
        }
        if (pick != null) {
            pick.setRecommended(true);
            pick.setRecommendReason(reason);
        }
    }

    private void markAreaRecommendation(List<StorageAreaOptionVo> rows, BigDecimal required, WmProductRecptLine line) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<StorageAreaOptionVo> usable = rows.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getMixingBlocked()))
                .filter(a -> a.getUnlimited() || a.getRemaining().compareTo(required) >= 0)
                .collect(Collectors.toList());
        StorageAreaOptionVo pick = usable.stream()
                .filter(a -> nullToZero(a.getSameQty()).compareTo(ZERO) > 0)
                .max(Comparator.comparing(a -> nullToZero(a.getSameQty())))
                .orElse(null);
        String reason = pick == null ? null : "该库位已有同物料同批次 " + plain(pick.getSameQty());
        if (pick == null) {
            pick = usable.stream()
                    .max(Comparator.comparing(a -> remainingForSort(a.getRemaining())))
                    .orElse(null);
            reason = pick == null ? null
                    : "余量充足（" + (pick.getUnlimited() ? "不限" : plain(pick.getRemaining())) + "）";
        }
        if (pick == null) {
            pick = rows.stream()
                    .filter(a -> !Boolean.TRUE.equals(a.getMixingBlocked()))
                    .findFirst().orElse(null);
            reason = pick == null ? null : "可用的默认库位";
        }
        if (pick != null) {
            pick.setRecommended(true);
            pick.setRecommendReason(reason);
        }
    }

    private String recommendAreaText(Long locationId, WmProductRecptLine line, BigDecimal qty, Long excludeAreaId) {
        List<StorageAreaOptionVo> areas = areaOptions(locationId, line, qty);
        StorageAreaOptionVo pick = areas.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getMixingBlocked()))
                .filter(a -> excludeAreaId == null || !a.getAreaId().equals(excludeAreaId))
                .findFirst()
                .orElse(null);
        if (pick != null) {
            return pick.getAreaName();
        }
        // 当前库区没有可用库位时，到同仓库的其它库区找推荐位置
        com.ktg.mes.wm.domain.WmStorageLocation location =
                locationService.selectWmStorageLocationByLocationId(locationId);
        if (location != null && location.getWarehouseId() != null) {
            List<StorageLocationOptionVo> locations = locationOptions(location.getWarehouseId(), line, qty);
            for (StorageLocationOptionVo loc : locations) {
                if (loc.getAreas() == null) {
                    continue;
                }
                for (StorageAreaOptionVo area : loc.getAreas()) {
                    if (Boolean.TRUE.equals(area.getMixingBlocked())) {
                        continue;
                    }
                    if (area.getUnlimited() || area.getRemaining().compareTo(qty) >= 0) {
                        return loc.getLocationName() + "/" + area.getAreaName();
                    }
                }
            }
        }
        return "其它仓库的可用库位（当前仓库无满足条件的库位）";
    }

    // ------------------------------------------------------------------ 混放

    private void checkAreaMixing(StorageAreaOptionVo area, WmProductRecptLine line) {
        area.setMixingBlocked(false);
        area.setMixingMessage(null);
        if (!mixingEnforced()) {
            return;
        }
        boolean sameBatchExists = nullToZero(area.getSameQty()).compareTo(ZERO) > 0;
        if (sameBatchExists) {
            // 已存在相同物料+批次，允许继续入库
            return;
        }
        BigDecimal pendingOtherItem = nullToZero(stockingMapper.selectPendingOtherItemQty(
                area.getAreaId(), line.getItemId(), null));
        BigDecimal pendingOtherBatch = nullToZero(stockingMapper.selectPendingOtherBatchQty(
                area.getAreaId(), line.getItemId(), line.getBatchCode(), null));

        boolean productConflict = nullToZero(area.getOtherItemQty()).compareTo(ZERO) > 0
                || pendingOtherItem.compareTo(ZERO) > 0;
        boolean batchConflict = nullToZero(area.getOtherBatchQty()).compareTo(ZERO) > 0
                || pendingOtherBatch.compareTo(ZERO) > 0;

        if (productConflict && !YES.equals(area.getProductMixing())) {
            String names = stockingMapper.selectOtherItemNames(area.getAreaId(), line.getItemId());
            if (StringUtils.isEmpty(names)) {
                names = "其它物料";
            }
            area.setMixingBlocked(true);
            area.setMixingMessage("该库位已有 " + names + "，不允许混放");
            return;
        }
        if (batchConflict && !YES.equals(area.getBatchMixing())) {
            String codes = stockingMapper.selectOtherBatchCodes(area.getAreaId(), line.getItemId(), line.getBatchCode());
            if (StringUtils.isEmpty(codes)) {
                codes = "(其它批次)";
            }
            area.setMixingBlocked(true);
            area.setMixingMessage("该库位已有 " + line.getItemName() + " 批次 " + codes + "，不允许混放");
        }
    }

    private boolean mixingEnforced() {
        String value = configService.selectConfigByKey(CONFIG_MIXING_ENFORCE);
        return value == null || !"false".equalsIgnoreCase(value.trim());
    }

    // ------------------------------------------------------------------ 余量

    private void fillRemaining(StorageLocationOptionVo row) {
        row.setOccupied(nullToZero(row.getOccupied()));
        row.setReserved(nullToZero(row.getReserved()));
        if (row.getCapacity() == null) {
            row.setUnlimited(true);
            row.setRemaining(null);
            return;
        }
        row.setUnlimited(false);
        BigDecimal remaining = row.getCapacity().subtract(row.getOccupied()).subtract(row.getReserved());
        row.setRemaining(remaining.compareTo(ZERO) < 0 ? ZERO : remaining);
    }

    private void fillAreaRemaining(StorageAreaOptionVo row) {
        row.setOccupied(nullToZero(row.getOccupied()));
        row.setReserved(nullToZero(row.getReserved()));
        if (row.getCapacity() == null) {
            row.setUnlimited(true);
            row.setRemaining(null);
            return;
        }
        row.setUnlimited(false);
        BigDecimal remaining = row.getCapacity().subtract(row.getOccupied()).subtract(row.getReserved());
        row.setRemaining(remaining.compareTo(ZERO) < 0 ? ZERO : remaining);
    }

    private BigDecimal remainingForSort(BigDecimal remaining) {
        return remaining == null ? new BigDecimal("999999999") : remaining;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
