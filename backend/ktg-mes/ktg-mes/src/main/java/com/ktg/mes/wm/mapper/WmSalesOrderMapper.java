package com.ktg.mes.wm.mapper;

import java.util.List;
import com.ktg.mes.wm.domain.WmSalesOrder;

/**
 * 客户销售订单Mapper接口
 *
 * @author ktg
 * @date 2026-11-14
 */
public interface WmSalesOrderMapper
{
    Long lockOrder(Long orderId);

    int finishWhenShipped(Long noticeId);

    /**
     * 查询客户销售订单
     *
     * @param orderId 订单主键
     * @return 客户销售订单
     */
    public WmSalesOrder selectWmSalesOrderByOrderId(Long orderId);

    /**
     * 查询客户销售订单列表
     *
     * @param wmSalesOrder 客户销售订单
     * @return 客户销售订单集合
     */
    public List<WmSalesOrder> selectWmSalesOrderList(WmSalesOrder wmSalesOrder);

    /**
     * 订单编号唯一性校验
     */
    public WmSalesOrder checkOrderCodeUnique(WmSalesOrder wmSalesOrder);

    /**
     * 新增客户销售订单
     */
    public int insertWmSalesOrder(WmSalesOrder wmSalesOrder);

    /**
     * 修改客户销售订单
     */
    public int updateWmSalesOrder(WmSalesOrder wmSalesOrder);

    /**
     * 批量删除客户销售订单
     */
    public int deleteWmSalesOrderByOrderIds(Long[] orderIds);
}
