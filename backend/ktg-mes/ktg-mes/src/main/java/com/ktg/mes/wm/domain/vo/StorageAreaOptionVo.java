package com.ktg.mes.wm.domain.vo;

import java.math.BigDecimal;

/**
 * 产品上架-库位候选（含容量/余量、混放开关与冲突信息）。
 */
public class StorageAreaOptionVo {

    /** 库位ID */
    private Long areaId;
    /** 库位编码 */
    private String areaCode;
    /** 库位名称 */
    private String areaName;
    /** 库区ID */
    private Long locationId;

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

    /** 是否允许产品混放 Y/N */
    private String productMixing;
    /** 是否允许批次混放 Y/N */
    private String batchMixing;

    /** 库位上该物料该批次已有数量 */
    private BigDecimal sameQty;
    /** 库位上其它物料占用数量 */
    private BigDecimal otherItemQty;
    /** 库位上同物料其它批次占用数量 */
    private BigDecimal otherBatchQty;
    /** 其它物料名称（用于提示） */
    private String otherItemNames;
    /** 同物料其它批次号（用于提示） */
    private String otherBatchCodes;

    /** 是否因混放被拦截 */
    private Boolean mixingBlocked;
    /** 混放提示 */
    private String mixingMessage;

    /** 是否推荐 */
    private Boolean recommended;
    /** 推荐原因 */
    private String recommendReason;

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
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

    public String getProductMixing() {
        return productMixing;
    }

    public void setProductMixing(String productMixing) {
        this.productMixing = productMixing;
    }

    public String getBatchMixing() {
        return batchMixing;
    }

    public void setBatchMixing(String batchMixing) {
        this.batchMixing = batchMixing;
    }

    public BigDecimal getSameQty() {
        return sameQty;
    }

    public void setSameQty(BigDecimal sameQty) {
        this.sameQty = sameQty;
    }

    public BigDecimal getOtherItemQty() {
        return otherItemQty;
    }

    public void setOtherItemQty(BigDecimal otherItemQty) {
        this.otherItemQty = otherItemQty;
    }

    public BigDecimal getOtherBatchQty() {
        return otherBatchQty;
    }

    public void setOtherBatchQty(BigDecimal otherBatchQty) {
        this.otherBatchQty = otherBatchQty;
    }

    public String getOtherItemNames() {
        return otherItemNames;
    }

    public void setOtherItemNames(String otherItemNames) {
        this.otherItemNames = otherItemNames;
    }

    public String getOtherBatchCodes() {
        return otherBatchCodes;
    }

    public void setOtherBatchCodes(String otherBatchCodes) {
        this.otherBatchCodes = otherBatchCodes;
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
}
