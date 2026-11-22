package com.ktg.mes.pro.domain;

import java.math.BigDecimal;

import com.ktg.common.core.domain.BaseEntity;

/**
 * 生产工单行（成品产出维度）
 *
 * <p>对应表 pro_workorder_line。一行代表工单要产出的一个物料；
 * 批次、BOM、工序等主链数据通过 workorder_line_id 挂到本行。</p>
 *
 * @author ktg
 * @date 2026-11-05
 */
public class ProWorkorderLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 行ID */
    private Long lineId;

    /** 工单ID */
    private Long workorderId;

    /** 工单内行号 */
    private Integer lineNo;

    /** 产出物料ID */
    private Long itemId;

    /** 产出物料编码（快照） */
    private String itemCode;

    /** 产出物料名称（快照） */
    private String itemName;

    /** 规格型号（快照） */
    private String itemSpc;

    /** 计量单位（快照） */
    private String unitOfMeasure;

    /** 单位名称（快照） */
    private String unitName;

    /** 计划数量 */
    private BigDecimal planQuantity;

    /** 已产数量 */
    private BigDecimal producedQuantity;

    /** 合格数量 */
    private BigDecimal qualifiedQuantity;

    /** 报废数量 */
    private BigDecimal scrappedQuantity;

    /** 批次策略 SINGLE/MULTI/NONE */
    private String batchStrategy;

    /** 工艺路线ID */
    private Long routeId;

    /** 状态 PREPARE/IN_PROGRESS/FINISHED/CANCELLED */
    private String status;

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public Long getWorkorderId() {
        return workorderId;
    }

    public void setWorkorderId(Long workorderId) {
        this.workorderId = workorderId;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemSpc() {
        return itemSpc;
    }

    public void setItemSpc(String itemSpc) {
        this.itemSpc = itemSpc;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public BigDecimal getPlanQuantity() {
        return planQuantity;
    }

    public void setPlanQuantity(BigDecimal planQuantity) {
        this.planQuantity = planQuantity;
    }

    public BigDecimal getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(BigDecimal producedQuantity) {
        this.producedQuantity = producedQuantity;
    }

    public BigDecimal getQualifiedQuantity() {
        return qualifiedQuantity;
    }

    public void setQualifiedQuantity(BigDecimal qualifiedQuantity) {
        this.qualifiedQuantity = qualifiedQuantity;
    }

    public BigDecimal getScrappedQuantity() {
        return scrappedQuantity;
    }

    public void setScrappedQuantity(BigDecimal scrappedQuantity) {
        this.scrappedQuantity = scrappedQuantity;
    }

    public String getBatchStrategy() {
        return batchStrategy;
    }

    public void setBatchStrategy(String batchStrategy) {
        this.batchStrategy = batchStrategy;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
