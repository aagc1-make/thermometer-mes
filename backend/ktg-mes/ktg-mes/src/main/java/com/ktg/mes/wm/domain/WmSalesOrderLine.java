package com.ktg.mes.wm.domain;

import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ktg.common.annotation.Excel;
import com.ktg.common.core.domain.BaseEntity;

/**
 * 客户销售订单行对象 wm_sales_order_line
 *
 * @author ktg
 * @date 2026-11-14
 */
public class WmSalesOrderLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 行ID */
    private Long lineId;

    /** 订单ID */
    private Long orderId;

    /** 产品物料ID */
    private Long itemId;

    /** 产品物料编码 */
    @Excel(name = "产品编码")
    private String itemCode;

    /** 产品物料名称 */
    @Excel(name = "产品名称")
    private String itemName;

    /** 规格型号 */
    @Excel(name = "规格型号")
    private String specification;

    /** 单位 */
    private String unitOfMeasure;

    /** 单位名称 */
    @Excel(name = "单位")
    private String unitName;

    /** 订单数量 */
    @Excel(name = "订单数量")
    private BigDecimal quantity;

    /** 单价 */
    @Excel(name = "单价")
    private BigDecimal price;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 已产数量 */
    @Excel(name = "已产数量")
    private BigDecimal quantityProduced;

    /** 已发数量 */
    @Excel(name = "已发数量")
    private BigDecimal quantityShipped;

    /** 生产工单ID */
    private Long workorderId;

    /** 生产工单号 */
    @Excel(name = "生产工单号")
    private String workorderCode;

    /** 出货检验单ID */
    private Long oqcId;

    /** 出货检验单号 */
    @Excel(name = "质检单号")
    private String oqcCode;

    /** 行状态 */
    @Excel(name = "行状态")
    private String status;

    /** 预留字段1 */
    private String attr1;

    /** 预留字段2 */
    private String attr2;

    /** 预留字段3 */
    private Long attr3;

    /** 预留字段4 */
    private Long attr4;

    public Long getLineId()
    {
        return lineId;
    }

    public void setLineId(Long lineId)
    {
        this.lineId = lineId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public String getItemCode()
    {
        return itemCode;
    }

    public void setItemCode(String itemCode)
    {
        this.itemCode = itemCode;
    }

    public String getItemName()
    {
        return itemName;
    }

    public void setItemName(String itemName)
    {
        this.itemName = itemName;
    }

    public String getSpecification()
    {
        return specification;
    }

    public void setSpecification(String specification)
    {
        this.specification = specification;
    }

    public String getUnitOfMeasure()
    {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure)
    {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getUnitName()
    {
        return unitName;
    }

    public void setUnitName(String unitName)
    {
        this.unitName = unitName;
    }

    public BigDecimal getQuantity()
    {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public BigDecimal getQuantityProduced()
    {
        return quantityProduced;
    }

    public void setQuantityProduced(BigDecimal quantityProduced)
    {
        this.quantityProduced = quantityProduced;
    }

    public BigDecimal getQuantityShipped()
    {
        return quantityShipped;
    }

    public void setQuantityShipped(BigDecimal quantityShipped)
    {
        this.quantityShipped = quantityShipped;
    }

    public Long getWorkorderId()
    {
        return workorderId;
    }

    public void setWorkorderId(Long workorderId)
    {
        this.workorderId = workorderId;
    }

    public String getWorkorderCode()
    {
        return workorderCode;
    }

    public void setWorkorderCode(String workorderCode)
    {
        this.workorderCode = workorderCode;
    }

    public Long getOqcId()
    {
        return oqcId;
    }

    public void setOqcId(Long oqcId)
    {
        this.oqcId = oqcId;
    }

    public String getOqcCode()
    {
        return oqcCode;
    }

    public void setOqcCode(String oqcCode)
    {
        this.oqcCode = oqcCode;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getAttr1()
    {
        return attr1;
    }

    public void setAttr1(String attr1)
    {
        this.attr1 = attr1;
    }

    public String getAttr2()
    {
        return attr2;
    }

    public void setAttr2(String attr2)
    {
        this.attr2 = attr2;
    }

    public Long getAttr3()
    {
        return attr3;
    }

    public void setAttr3(Long attr3)
    {
        this.attr3 = attr3;
    }

    public Long getAttr4()
    {
        return attr4;
    }

    public void setAttr4(Long attr4)
    {
        this.attr4 = attr4;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("lineId", getLineId())
            .append("orderId", getOrderId())
            .append("itemId", getItemId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("quantity", getQuantity())
            .append("price", getPrice())
            .append("amount", getAmount())
            .append("quantityShipped", getQuantityShipped())
            .append("workorderId", getWorkorderId())
            .append("workorderCode", getWorkorderCode())
            .append("oqcId", getOqcId())
            .append("oqcCode", getOqcCode())
            .append("status", getStatus())
            .append("remark", getRemark())
            .toString();
    }
}
