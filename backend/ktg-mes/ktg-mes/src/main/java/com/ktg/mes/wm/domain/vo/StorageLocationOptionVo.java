package com.ktg.mes.wm.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品上架-库区候选（含容量/余量与推荐标记）。
 *
 * <p>余量口径：剩余 = 容量 - 已占用库存 - 未过账上架预占。
 * 容量为空表示不限量，remaining 返回 null，前端展示“不限”。</p>
 */
public class StorageLocationOptionVo {

    /** 库区ID */
    private Long locationId;
    /** 库区编码 */
    private String locationCode;
    /** 库区名称 */
    private String locationName;
    /** 仓库ID */
    private Long warehouseId;
    /** 仓库编码 */
    private String warehouseCode;
    /** 仓库名称 */
    private String warehouseName;
    /** 仓库类型 RAW/LINE/FG */
    private String warehouseType;

    /** 容量（max_qty） */
    private BigDecimal capacity;
    /** 已占用库存数量 */
    private BigDecimal occupied;
    /** 未过账上架预占数量 */
    private BigDecimal reserved;
    /** 余量，容量为空时为 null（不限） */
    private BigDecimal remaining;
    /** 是否不限量 */
    private Boolean unlimited;
    /** 同物料同批次已存放数量（推荐权重1） */
    private BigDecimal sameItemBatchQty;

    /** 是否推荐 */
    private Boolean recommended;
    /** 推荐原因 */
    private String recommendReason;
    /** 是否因混放被拦截 */
    private Boolean mixingBlocked;
    /** 混放提示 */
    private String mixingMessage;
    /** 该库区下可用库位 */
    private List<StorageAreaOptionVo> areas;

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseType() {
        return warehouseType;
    }

    public void setWarehouseType(String warehouseType) {
        this.warehouseType = warehouseType;
    }

    public BigDecimal getCapacity() {
        return capacity;
    }

    public void setCapacity(BigDecimal capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getOccupied() {
        return occupied;
    }

    public void setOccupied(BigDecimal occupied) {
        this.occupied = occupied;
    }

    public BigDecimal getReserved() {
        return reserved;
    }

    public void setReserved(BigDecimal reserved) {
        this.reserved = reserved;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void setRemaining(BigDecimal remaining) {
        this.remaining = remaining;
    }

    public Boolean getUnlimited() {
        return unlimited;
    }

    public void setUnlimited(Boolean unlimited) {
        this.unlimited = unlimited;
    }

    public BigDecimal getSameItemBatchQty() {
        return sameItemBatchQty;
    }

    public void setSameItemBatchQty(BigDecimal sameItemBatchQty) {
        this.sameItemBatchQty = sameItemBatchQty;
    }

    public Boolean getRecommended() {
        return recommended;
    }

    public void setRecommended(Boolean recommended) {
        this.recommended = recommended;
    }

    public String getRecommendReason() {
        return recommendReason;
    }

    public void setRecommendReason(String recommendReason) {
        this.recommendReason = recommendReason;
    }

    public Boolean getMixingBlocked() {
        return mixingBlocked;
    }

    public void setMixingBlocked(Boolean mixingBlocked) {
        this.mixingBlocked = mixingBlocked;
    }

    public String getMixingMessage() {
        return mixingMessage;
    }

    public void setMixingMessage(String mixingMessage) {
        this.mixingMessage = mixingMessage;
    }

    public List<StorageAreaOptionVo> getAreas() {
        return areas;
    }

    public void setAreas(List<StorageAreaOptionVo> areas) {
        this.areas = areas;
    }
}
