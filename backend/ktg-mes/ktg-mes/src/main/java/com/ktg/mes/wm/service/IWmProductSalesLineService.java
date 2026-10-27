package com.ktg.mes.wm.service;

import java.util.List;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import com.ktg.mes.wm.domain.WmRtSalesLine;

/**
 * 产品销售出库行Service接口
 * 
 * @author yinjinlu
 * @date 2022-10-05
 */
public interface IWmProductSalesLineService
{
    /**
     * 查询产品销售出库行
     * 
     * @param lineId 产品销售出库行主键
     * @return 产品销售出库行
     */
    public WmProductSalesLine selectWmProductSalesLineByLineId(Long lineId);

    /**
     * 查询产品销售出库行（带拣货数量）
     *
     * @param lineId 产品销售出库行主键
     * @return 产品销售出库行
     */
    public WmProductSalesLine selectWmProductSalesLineWithQuantityByLineId(Long lineId);

    /**
     * 查询产品销售出库行列表
     * 
     * @param wmProductSalesLine 产品销售出库行
     * @return 产品销售出库行集合
     */
    public List<WmProductSalesLine> selectWmProductSalesLineList(WmProductSalesLine wmProductSalesLine);

    /**
     * 查询产品销售出库行列表（含拣货数量）
     *
     * @param wmProductSalesLine 产品销售出库行
     * @return 产品销售出库行集合
     */
    public List<WmProductSalesLine> selectWmProductSalesLineWithQuantityList(WmProductSalesLine wmProductSalesLine);


    /**
     * 查询产品销售出库行列表
     *
     * @param wmProductSalesLine 产品销售出库行（包含明细）
     * @return 产品销售出库行集合
     */
    public List<WmProductSalesLine> selectWmProductSalesLineWithDetailList(WmProductSalesLine wmProductSalesLine);

    /**
     * 新增产品销售出库行
     * 
     * @param wmProductSalesLine 产品销售出库行
     * @return 结果
     */
    public int insertWmProductSalesLine(WmProductSalesLine wmProductSalesLine);

    /**
     * 修改产品销售出库行
     * 
     * @param wmProductSalesLine 产品销售出库行
     * @return 结果
     */
    public int updateWmProductSalesLine(WmProductSalesLine wmProductSalesLine);

    /**
     * 出货检验完成后的受控回写（阶段1复审修复）
     *
     * 只允许写入 oqcId / oqcCode / qualityStatus 三个质量字段，
     * 不走"草稿才可编辑"的通用校验，因为检验完成发生在销售单已提交待检之后。
     * 物料、批次、数量、父单归属一律不接收客户端值。
     *
     * @param wmProductSalesLine 至少包含 lineId 与质量字段
     * @return 结果
     */
    public int writeBackOqcResult(WmProductSalesLine wmProductSalesLine);

    /**
     * 按出货检验合格量缩减出库数量（第6轮复审：禁止超检验合格量执行）
     *
     * 只允许调小，不允许调大；用于部分合格时把出库数量压到合格量。
     *
     * @param lineId       出库行主键
     * @param qualifiedQty 本次检验合格数量
     * @return 结果
     */
    public int applyQualifiedQuantity(Long lineId, java.math.BigDecimal qualifiedQty);

    /**
     * 批量删除产品销售出库行
     * 
     * @param lineIds 需要删除的产品销售出库行主键集合
     * @return 结果
     */
    public int deleteWmProductSalesLineByLineIds(Long[] lineIds);

    /**
     * 删除产品销售出库行信息
     * 
     * @param lineId 产品销售出库行主键
     * @return 结果
     */
    public int deleteWmProductSalesLineByLineId(Long lineId);

    /**
     * 根据出库单头删除所有行
     * @param salesId
     * @return
     */
    public int deleteBySalesId(Long salesId);
}
