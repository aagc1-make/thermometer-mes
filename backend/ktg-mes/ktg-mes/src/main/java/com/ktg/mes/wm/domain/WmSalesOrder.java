package com.ktg.mes.wm.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ktg.common.annotation.Excel;
import com.ktg.common.core.domain.BaseEntity;

/**
 * 客户销售订单对象 wm_sales_order
 *
 * @author ktg
 * @date 2026-11-14
 */
public class WmSalesOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 订单编号 */
    @Excel(name = "订单编号")
    private String orderCode;

    /** 订单名称 */
    @Excel(name = "订单名称")
    private String orderName;

    /** 客户ID */
    private Long clientId;

    /** 客户编码 */
    @Excel(name = "客户编码")
    private String clientCode;

    /** 客户名称 */
    @Excel(name = "客户名称")
    private String clientName;

    /** 客户简称 */
    private String clientNick;

    /** 下单日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "下单日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date orderDate;

    /** 期望交期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "期望交期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date deliveryDate;

    /** 收货人 */
    @Excel(name = "收货人")
    private String recipient;

    /** 联系方式 */
    @Excel(name = "联系方式")
    private String tel;

    /** 收货地址 */
    @Excel(name = "收货地址")
    private String address;

    /** 订单总数量 */
    @Excel(name = "订单总数量")
    private BigDecimal totalQuantity;

    /** 订单总金额 */
    @Excel(name = "订单总金额")
    private BigDecimal totalAmount;

    /** 单据状态 */
    @Excel(name = "单据状态")
    private String status;

    /** 发货通知单ID */
    private Long noticeId;

    /** 发货通知单号 */
    @Excel(name = "发货通知单号")
    private String noticeCode;

    /** 预留字段1 */
    private String attr1;

    /** 预留字段2 */
    private String attr2;

    /** 预留字段3 */
    private Long attr3;

    /** 预留字段4 */
    private Long attr4;

    /** 订单行 */
    private List<WmSalesOrderLine> lines;

    /** 已生成工单号（逗号分隔，列表展示用） */
    private String workorderCodes;

    /** 已生成质检单号（逗号分隔，列表展示用） */
    private String oqcCodes;

    public String getWorkorderCodes()
    {
        return workorderCodes;
    }

    public void setWorkorderCodes(String workorderCodes)
    {
        this.workorderCodes = workorderCodes;
    }

    public String getOqcCodes()
    {
        return oqcCodes;
    }

    public void setOqcCodes(String oqcCodes)
    {
        this.oqcCodes = oqcCodes;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public String getOrderCode()
    {
        return orderCode;
    }

    public void setOrderCode(String orderCode)
    {
        this.orderCode = orderCode;
    }

    public String getOrderName()
    {
        return orderName;
    }

    public void setOrderName(String orderName)
    {
        this.orderName = orderName;
    }

    public Long getClientId()
    {
        return clientId;
    }

    public void setClientId(Long clientId)
    {
        this.clientId = clientId;
    }

    public String getClientCode()
    {
        return clientCode;
    }

    public void setClientCode(String clientCode)
    {
        this.clientCode = clientCode;
    }

    public String getClientName()
    {
        return clientName;
    }

    public void setClientName(String clientName)
    {
        this.clientName = clientName;
    }

    public String getClientNick()
    {
        return clientNick;
    }

    public void setClientNick(String clientNick)
    {
        this.clientNick = clientNick;
    }

    public Date getOrderDate()
    {
        return orderDate;
    }

    public void setOrderDate(Date orderDate)
    {
        this.orderDate = orderDate;
    }

    public Date getDeliveryDate()
    {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate)
    {
        this.deliveryDate = deliveryDate;
    }

    public String getRecipient()
    {
        return recipient;
    }

    public void setRecipient(String recipient)
    {
        this.recipient = recipient;
    }

    public String getTel()
    {
        return tel;
    }

    public void setTel(String tel)
    {
        this.tel = tel;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public BigDecimal getTotalQuantity()
    {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity)
    {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getNoticeId()
    {
        return noticeId;
    }

    public void setNoticeId(Long noticeId)
    {
        this.noticeId = noticeId;
    }

    public String getNoticeCode()
    {
        return noticeCode;
    }

    public void setNoticeCode(String noticeCode)
    {
        this.noticeCode = noticeCode;
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

    public List<WmSalesOrderLine> getLines()
    {
        return lines;
    }

    public void setLines(List<WmSalesOrderLine> lines)
    {
        this.lines = lines;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("orderId", getOrderId())
            .append("orderCode", getOrderCode())
            .append("orderName", getOrderName())
            .append("clientId", getClientId())
            .append("clientCode", getClientCode())
            .append("clientName", getClientName())
            .append("orderDate", getOrderDate())
            .append("deliveryDate", getDeliveryDate())
            .append("totalQuantity", getTotalQuantity())
            .append("totalAmount", getTotalAmount())
            .append("status", getStatus())
            .append("noticeId", getNoticeId())
            .append("noticeCode", getNoticeCode())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
