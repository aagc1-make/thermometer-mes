package com.ktg.mes.wm.service;

import java.util.List;

import com.ktg.common.core.domain.AjaxResult;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.tx.ProductSalesTxBean;

/**
 * 销售出库单Service接口
 * 
 * @author yinjinlu
 * @date 2022-10-04
 */
public interface IWmProductSalesService
{
    /**
     * 查询销售出库单
     * 
     * @param salesId 销售出库单主键
     * @return 销售出库单
     */
    public WmProductSales selectWmProductSalesBySalesId(Long salesId);

    /**
     * 查询销售出库单列表
     * 
     * @param wmProductSales 销售出库单
     * @return 销售出库单集合
     */
    public List<WmProductSales> selectWmProductSalesList(WmProductSales wmProductSales);


    /**
     * 获取产品销售出库事务Bean
     * @param salesId
     * @return
     */
    public List<ProductSalesTxBean> getTxBeans(Long salesId);

    /**
     * 检查编号唯一性
     * @param wmProductSales
     * @return
     */
    public String checkUnique(WmProductSales wmProductSales);

    /**
     * 新增销售出库单
     * 
     * @param wmProductSales 销售出库单
     * @return 结果
     */
    public int insertWmProductSales(WmProductSales wmProductSales);

    /**
     * 修改销售出库单
     * 
     * @param wmProductSales 销售出库单
     * @return 结果
     */
    public int updateWmProductSales(WmProductSales wmProductSales);

    /**
     * 提交待检（阶段1复审修复①）
     *
     * 专用业务方法：原子地校验状态、逐行标记待检、并把主单推进到 UNCHECK。
     * 不走通用 edit，避免"父单已离开草稿"时被草稿保护误拦。
     *
     * @param salesId 销售出库单主键
     * @return 结果（1 表示已提交待检，0 表示无需检验已直接放行）
     */
    public int submitForInspection(Long salesId);

    /**
     * 批量删除销售出库单
     * 
     * @param salesIds 需要删除的销售出库单主键集合
     * @return 结果
     */
    public int deleteWmProductSalesBySalesIds(Long[] salesIds);

    /**
     * 删除销售出库单信息
     * 
     * @param salesId 销售出库单主键
     * @return 结果
     */
    public int deleteWmProductSalesBySalesId(Long salesId);

    /**
     * 根据客户 id 获取产品清单数据
     * @param clientId
     * @return
     */
    AjaxResult getItem(Long clientId);

    /**
     * 根据客户 id 获取销售记录数据
     * @param clientId
     * @return
     */
    AjaxResult getSaleRecord(Long clientId);
}
