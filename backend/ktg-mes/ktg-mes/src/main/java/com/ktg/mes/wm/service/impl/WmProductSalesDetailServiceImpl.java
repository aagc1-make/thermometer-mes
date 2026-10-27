package com.ktg.mes.wm.service.impl;

import java.util.List;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import com.ktg.mes.wm.mapper.WmProductSalesDetailMapper;
import com.ktg.mes.wm.mapper.WmProductSalesLineMapper;
import com.ktg.mes.wm.mapper.WmProductSalesMapper;
import com.ktg.mes.wm.domain.WmProductSalesDetail;
import com.ktg.mes.wm.service.IWmProductSalesDetailService;

/**
 * 产品销售出库记录明细Service业务层处理
 *
 * 联动优化（阶段1复审）：拣货明细同样受父单状态保护，
 * 已提交待检/已执行的销售单不允许再增删改拣货明细。
 */
@Service
public class WmProductSalesDetailServiceImpl implements IWmProductSalesDetailService 
{
    @Autowired
    private WmProductSalesDetailMapper wmProductSalesDetailMapper;

    @Autowired
    private WmProductSalesLineMapper wmProductSalesLineMapper;

    @Autowired
    private WmProductSalesMapper wmProductSalesMapper;

    /** 通过出库行找到销售单，并校验其仍处于可编辑状态 */
    private void requireEditableByLine(Long lineId) {
        if (lineId == null) {
            throw new ServiceException("拣货明细未指定出库行");
        }
        WmProductSalesLine line = wmProductSalesLineMapper.selectWmProductSalesLineByLineId(lineId);
        if (line == null) {
            throw new ServiceException("出库行不存在，操作被拒绝");
        }
        WmProductSales sales = wmProductSalesMapper.selectWmProductSalesBySalesId(line.getSalesId());
        if (sales == null) {
            throw new ServiceException("销售单不存在，操作被拒绝");
        }
        // 允许状态：草稿、待提交，以及真实的"待拣货(UNSTOCK)"阶段——
        // 拣货明细本身就是在 UNSTOCK 阶段录入的，之前漏了该状态导致正常拣货被拦（阶段1复审③）
        if (!UserConstants.ORDER_STATUS_PREPARE.equals(sales.getStatus())
                && !UserConstants.ORDER_STATUS_APPROVING.equals(sales.getStatus())
                && !UserConstants.PRODUCT_SALES_STATUS_UNSTOCK.equals(sales.getStatus())) {
            throw new ServiceException("销售单已进入检验、发运或已执行阶段，禁止修改拣货明细");
        }
        if (line.getOqcId() != null) {
            throw new ServiceException("该出库行已生成出货检验，禁止修改拣货明细");
        }
    }

    /**
     * 查询产品销售出库记录明细
     * 
     * @param detailId 产品销售出库记录明细主键
     * @return 产品销售出库记录明细
     */
    @Override
    public WmProductSalesDetail selectWmProductSalesDetailByDetailId(Long detailId)
    {
        return wmProductSalesDetailMapper.selectWmProductSalesDetailByDetailId(detailId);
    }

    /**
     * 查询产品销售出库记录明细列表
     * 
     * @param wmProductSalesDetail 产品销售出库记录明细
     * @return 产品销售出库记录明细
     */
    @Override
    public List<WmProductSalesDetail> selectWmProductSalesDetailList(WmProductSalesDetail wmProductSalesDetail)
    {
        return wmProductSalesDetailMapper.selectWmProductSalesDetailList(wmProductSalesDetail);
    }

    @Override
    public String checkQuantity(Long lineId) {
        return wmProductSalesDetailMapper.checkQuantity(lineId);
    }

    /**
     * 新增产品销售出库记录明细
     * 
     * @param wmProductSalesDetail 产品销售出库记录明细
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWmProductSalesDetail(WmProductSalesDetail wmProductSalesDetail)
    {
        requireEditableByLine(wmProductSalesDetail.getLineId());
        wmProductSalesDetail.setCreateTime(DateUtils.getNowDate());
        return wmProductSalesDetailMapper.insertWmProductSalesDetail(wmProductSalesDetail);
    }

    /**
     * 修改产品销售出库记录明细
     * 
     * @param wmProductSalesDetail 产品销售出库记录明细
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWmProductSalesDetail(WmProductSalesDetail wmProductSalesDetail)
    {
        // 阶段1复审④：原实现漏了父单状态检查，已执行单据仍能改拣货明细。
        // 以库中既有明细的 lineId 为准做校验，避免客户端伪造 lineId 绕过。
        if (wmProductSalesDetail.getDetailId() == null) {
            throw new ServiceException("拣货明细ID不能为空");
        }
        WmProductSalesDetail old = wmProductSalesDetailMapper
                .selectWmProductSalesDetailByDetailId(wmProductSalesDetail.getDetailId());
        if (old == null) {
            throw new ServiceException("拣货明细不存在，不能修改");
        }
        requireEditableByLine(old.getLineId());
        wmProductSalesDetail.setLineId(old.getLineId());
        wmProductSalesDetail.setUpdateTime(DateUtils.getNowDate());
        return wmProductSalesDetailMapper.updateWmProductSalesDetail(wmProductSalesDetail);
    }

    /**
     * 批量删除产品销售出库记录明细
     * 
     * @param detailIds 需要删除的产品销售出库记录明细主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmProductSalesDetailByDetailIds(Long[] detailIds)
    {
        if (detailIds != null) {
            for (Long detailId : detailIds) {
                WmProductSalesDetail d = wmProductSalesDetailMapper
                        .selectWmProductSalesDetailByDetailId(detailId);
                if (d != null) {
                    requireEditableByLine(d.getLineId());
                }
            }
        }
        return wmProductSalesDetailMapper.deleteWmProductSalesDetailByDetailIds(detailIds);
    }

    /**
     * 删除产品销售出库记录明细信息
     * 
     * @param detailId 产品销售出库记录明细主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmProductSalesDetailByDetailId(Long detailId)
    {
        WmProductSalesDetail d = wmProductSalesDetailMapper.selectWmProductSalesDetailByDetailId(detailId);
        if (d != null) {
            requireEditableByLine(d.getLineId());
        }
        return wmProductSalesDetailMapper.deleteWmProductSalesDetailByDetailId(detailId);
    }
}
