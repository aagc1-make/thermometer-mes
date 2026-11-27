package com.ktg.mes.wm.mapper;

import java.util.List;
import com.ktg.mes.wm.domain.WmSalesOrderLine;

/**
 * 客户销售订单行Mapper接口
 *
 * @author ktg
 * @date 2026-11-14
 */
public interface WmSalesOrderLineMapper
{
    int refreshShippedForNotice(Long noticeId);

    /**
     * 查询客户销售订单行
     */
    public WmSalesOrderLine selectWmSalesOrderLineByLineId(Long lineId);

    /**
     * 查询客户销售订单行列表
     */
    public List<WmSalesOrderLine> selectWmSalesOrderLineList(WmSalesOrderLine wmSalesOrderLine);

    /**
     * 新增客户销售订单行
     */
    public int insertWmSalesOrderLine(WmSalesOrderLine wmSalesOrderLine);

    /**
     * 修改客户销售订单行
     */
    public int updateWmSalesOrderLine(WmSalesOrderLine wmSalesOrderLine);

    /**
     * 按订单删除行
     */
    public int deleteWmSalesOrderLineByOrderId(Long orderId);

    /**
     * 批量删除行
     */
    public int deleteWmSalesOrderLineByLineIds(Long[] lineIds);
}
