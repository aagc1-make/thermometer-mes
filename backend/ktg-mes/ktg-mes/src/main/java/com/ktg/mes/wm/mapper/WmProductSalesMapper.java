package com.ktg.mes.wm.mapper;

import java.util.List;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import com.ktg.mes.wm.domain.tx.ProductSalesTxBean;
import com.ktg.mes.wm.domain.vo.WorkorderProductSaleVO;
import org.apache.ibatis.annotations.Param;

/**
 * 销售出库单Mapper接口
 * 
 * @author yinjinlu
 * @date 2022-10-04
 */
public interface WmProductSalesMapper
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
     * 检查编号唯一性
     * @param wmProductSales
     * @return
     */
    public WmProductSales checkUnique(WmProductSales wmProductSales);

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
     * 删除销售出库单
     * 
     * @param salesId 销售出库单主键
     * @return 结果
     */
    public int deleteWmProductSalesBySalesId(Long salesId);

    /**
     * 批量删除销售出库单
     * 
     * @param salesIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteWmProductSalesBySalesIds(Long[] salesIds);

    /**
     * 获取产品销售出库事务Bean
     * @param salesId
     * @return
     */
    public List<ProductSalesTxBean> getTxBeans(Long salesId);

    /**
     * 根据客户id获取出库产品信息
     * @param clientId
     * @return
     */
    List<WmProductSalesLine> getItem(@Param("clientId") Long clientId);

    /**
     * 根据客户 id 获取销售记录数据
     * @param clientId
     * @return
     */
    List<WmProductSales> getSaleRecord(@Param("clientId") Long clientId);

    /**
     * 查询"工单生成的产品"可售清单
     *
     * <p>销售出库单添加行时使用：按「工单 + 产品」汇总已入库数量，扣减未取消的销售出库行数量，
     * 返回可售数量与最近入库批次。只读查询，不写任何单据。</p>
     *
     * @param query 过滤条件（工单编号/产品编码/产品名称/是否只看可售/排除行）
     * @return 工单成品可售清单
     */
    List<WorkorderProductSaleVO> selectWorkorderProductForSale(WorkorderProductSaleVO query);
}
