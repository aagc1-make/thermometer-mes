package com.ktg.mes.wm.service;

import java.util.List;
import java.util.Map;

import com.ktg.mes.wm.domain.WmSalesOrder;

/**
 * 客户销售订单Service接口
 *
 * @author ktg
 * @date 2026-11-14
 */
public interface IWmSalesOrderService
{
    /**
     * 查询客户销售订单（含行）
     */
    public WmSalesOrder selectWmSalesOrderByOrderId(Long orderId);

    /**
     * 查询客户销售订单列表
     */
    public List<WmSalesOrder> selectWmSalesOrderList(WmSalesOrder wmSalesOrder);

    /**
     * 订单编号唯一性校验
     */
    public String checkOrderCodeUnique(WmSalesOrder wmSalesOrder);

    /**
     * 新增客户销售订单（同时保存订单行）
     */
    public int insertWmSalesOrder(WmSalesOrder wmSalesOrder);

    /**
     * 修改客户销售订单（仅草稿可改，整体重写订单行）
     */
    public int updateWmSalesOrder(WmSalesOrder wmSalesOrder);

    /**
     * 批量删除客户销售订单（仅草稿且未生成下游单据）
     */
    public int deleteWmSalesOrderByOrderIds(Long[] orderIds);

    /**
     * 确认下单
     */
    public int confirmOrder(Long orderId, String operator);

    /**
     * 按订单行生成生产工单（幂等，返回已生成/新生成的工单号）
     */
    public Map<String, Object> generateWorkorders(Long orderId, String operator);

    /**
     * 按订单生成发货通知单（幂等，返回通知单号）
     */
    public Map<String, Object> generateNotice(Long orderId, String operator);

    /**
     * 按订单行生成出货检验单（幂等，返回检验单号）
     */
    public Map<String, Object> generateOqc(Long orderId, String operator);
}
