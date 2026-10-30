package com.ktg.mes.wm.service.impl;

import java.math.BigDecimal;
import java.util.List;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.DateUtils;
import com.ktg.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesDetail;
import com.ktg.mes.wm.mapper.WmProductSalesDetailMapper;
import com.ktg.mes.wm.mapper.WmProductSalesLineMapper;
import com.ktg.mes.wm.mapper.WmProductSalesMapper;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import com.ktg.mes.wm.service.IWmProductSalesLineService;

/**
 * 产品销售出库行Service业务层处理
 *
 * 联动优化（阶段1复审）：把"父单状态保护 + 字段白名单 + 已检行冻结"下沉到 Service，
 * PC 端与移动端共用同一套规则，Controller 不再各自过滤。
 */
@Service
public class WmProductSalesLineServiceImpl implements IWmProductSalesLineService
{
    @Autowired
    private WmProductSalesLineMapper wmProductSalesLineMapper;

    @Autowired
    private WmProductSalesMapper wmProductSalesMapper;

    @Autowired
    private WmProductSalesDetailMapper wmProductSalesDetailMapper;

    /* ==================== 保护规则（Service 层统一） ==================== */

    /** 明细只能在草稿/待提交的销售单下被增删改 */
    private WmProductSales requireEditableParent(Long salesId) {
        if (salesId == null) {
            throw new ServiceException("出库行未指定所属销售单，操作被拒绝");
        }
        WmProductSales parent = wmProductSalesMapper.selectWmProductSalesBySalesId(salesId);
        if (parent == null) {
            throw new ServiceException("销售单不存在，操作被拒绝");
        }
        String status = parent.getStatus();
        if (!UserConstants.ORDER_STATUS_PREPARE.equals(status)
                && !UserConstants.ORDER_STATUS_APPROVING.equals(status)) {
            throw new ServiceException("销售单已提交待检或已执行，禁止增删改明细，请走销售退货流程");
        }
        return parent;
    }

    private static boolean numChanged(BigDecimal incoming, BigDecimal old) {
        return incoming != null && (old == null || incoming.compareTo(old) != 0);
    }

    private static boolean objChanged(Object incoming, Object old) {
        return incoming != null && !incoming.equals(old);
    }

    /** 已生成出货检验的行：物料、批次、数量一律冻结，避免"已检后改料改批" */
    private void assertInspectedLineFrozen(WmProductSalesLine incoming, WmProductSalesLine old) {
        if (old.getOqcId() == null) {
            return;
        }
        boolean changed = objChanged(incoming.getItemId(), old.getItemId())
                || objChanged(incoming.getBatchId(), old.getBatchId())
                || objChanged(incoming.getBatchCode(), old.getBatchCode())
                || numChanged(incoming.getQuantitySales(), old.getQuantitySales());
        if (changed) {
            throw new ServiceException("该出库行已生成出货检验，禁止修改物料/批次/数量，请先作废检验单");
        }
    }

    /**
     * 查询产品销售出库行
     * 
     * @param lineId 产品销售出库行主键
     * @return 产品销售出库行
     */
    @Override
    public WmProductSalesLine selectWmProductSalesLineByLineId(Long lineId)
    {
        return wmProductSalesLineMapper.selectWmProductSalesLineByLineId(lineId);
    }

    @Override
    public WmProductSalesLine selectWmProductSalesLineWithQuantityByLineId(Long lineId) {
        return wmProductSalesLineMapper.selectWmProductSalesLineWithQuantityByLineId(lineId);
    }

    /**
     * 查询产品销售出库行列表
     * 
     * @param wmProductSalesLine 产品销售出库行
     * @return 产品销售出库行
     */
    @Override
    public List<WmProductSalesLine> selectWmProductSalesLineList(WmProductSalesLine wmProductSalesLine)
    {
        return wmProductSalesLineMapper.selectWmProductSalesLineList(wmProductSalesLine);
    }

    @Override
    public List<WmProductSalesLine> selectWmProductSalesLineWithQuantityList(WmProductSalesLine wmProductSalesLine) {
        return wmProductSalesLineMapper.selectWmProductSalesLineWithQuantityList(wmProductSalesLine);
    }

    @Override
    public List<WmProductSalesLine> selectWmProductSalesLineWithDetailList(WmProductSalesLine wmProductSalesLine) {
        return wmProductSalesLineMapper.selectWmProductSalesLineWithDetailList(wmProductSalesLine);
    }

    /**
     * 新增产品销售出库行
     * 
     * @param wmProductSalesLine 产品销售出库行
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWmProductSalesLine(WmProductSalesLine wmProductSalesLine)
    {
        requireEditableParent(wmProductSalesLine.getSalesId());
        if (wmProductSalesLine.getQuantitySales() != null
                && wmProductSalesLine.getQuantitySales().signum() <= 0) {
            throw new ServiceException("出库数量必须大于零");
        }
        // 质量结果字段只能由检验流程写入；oqcCheck 表示"该行是否需要 OQC"，
        // 属于业务属性，必须保留调用方（PC/移动端）提交的值，不能强制改成免检。
        wmProductSalesLine.setOqcId(null);
        wmProductSalesLine.setOqcCode(null);
        wmProductSalesLine.setQualityStatus(null);
        wmProductSalesLine.setCreateTime(DateUtils.getNowDate());
        return wmProductSalesLineMapper.insertWmProductSalesLine(wmProductSalesLine);
    }

    /**
     * 修改产品销售出库行
     * 
     * @param wmProductSalesLine 产品销售出库行
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWmProductSalesLine(WmProductSalesLine wmProductSalesLine)
    {
        if (wmProductSalesLine.getLineId() == null) {
            throw new ServiceException("出库行ID不能为空");
        }
        WmProductSalesLine old = wmProductSalesLineMapper
                .selectWmProductSalesLineByLineId(wmProductSalesLine.getLineId());
        if (old == null) {
            throw new ServiceException("出库行不存在，不能修改");
        }
        requireEditableParent(old.getSalesId());
        assertInspectedLineFrozen(wmProductSalesLine, old);
        if (wmProductSalesLine.getQuantitySales() != null
                && wmProductSalesLine.getQuantitySales().signum() <= 0) {
            throw new ServiceException("出库数量必须大于零");
        }
        // 字段白名单：父单、质量字段一律以库中值为准，客户端提交值丢弃
        wmProductSalesLine.setSalesId(old.getSalesId());
        wmProductSalesLine.setOqcId(old.getOqcId());
        wmProductSalesLine.setOqcCode(old.getOqcCode());
        wmProductSalesLine.setOqcCheck(old.getOqcCheck());
        wmProductSalesLine.setQualityStatus(old.getQualityStatus());
        wmProductSalesLine.setUpdateTime(DateUtils.getNowDate());
        return wmProductSalesLineMapper.updateWmProductSalesLine(wmProductSalesLine);
    }

    /**
     * 出货检验完成后的受控回写（阶段1复审修复①②）
     *
     * 与通用 update 的区别：
     *  - 不做"父单必须草稿"的校验，因为 OQC 完成时销售单已进入待检/待发运；
     *  - 只写质量字段，物料/批次/数量/父单/需检标志一律沿用库中值；
     *  - 行必须存在，避免回写到不存在的行。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int writeBackOqcResult(WmProductSalesLine qcResult)
    {
        if (qcResult == null || qcResult.getLineId() == null) {
            throw new ServiceException("出货检验回写缺少出库行ID");
        }
        WmProductSalesLine old = wmProductSalesLineMapper
                .selectWmProductSalesLineByLineId(qcResult.getLineId());
        if (old == null) {
            throw new ServiceException("出库行不存在，无法回写检验结果");
        }
        WmProductSalesLine update = new WmProductSalesLine();
        update.setLineId(old.getLineId());
        update.setOqcId(qcResult.getOqcId());
        update.setOqcCode(qcResult.getOqcCode());
        update.setQualityStatus(qcResult.getQualityStatus());
        update.setUpdateTime(DateUtils.getNowDate());
        update.setUpdateBy(qcResult.getUpdateBy());
        return wmProductSalesLineMapper.updateWmProductSalesLine(update);
    }

    /**
     * 按出货检验合格量缩减出库数量（第6轮复审：禁止超检验合格量执行）
     *
     * 第7轮复审补充：必须同步调整拣货明细，否则 checkQuantity 仍为原数量，
     * 销售执行会报"数量不一致"并形成死结（检验后又禁止手工改拣货）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int applyQualifiedQuantity(Long lineId, BigDecimal qualifiedQty)
    {
        if (lineId == null) {
            throw new ServiceException("缩减出库数量时缺少出库行ID");
        }
        if (qualifiedQty == null || qualifiedQty.signum() < 0) {
            throw new ServiceException("合格数量不能为空或负数");
        }
        WmProductSalesLine old = wmProductSalesLineMapper.selectWmProductSalesLineByLineId(lineId);
        if (old == null) {
            throw new ServiceException("出库行不存在，无法缩减数量");
        }
        if (old.getQuantitySales() != null && qualifiedQty.compareTo(old.getQuantitySales()) > 0) {
            throw new ServiceException("缩减后的数量不能大于原出库数量");
        }

        // ① 同步拣货明细：按原有顺序保留到合格量，超出部分缩减/删除
        WmProductSalesDetail param = new WmProductSalesDetail();
        param.setLineId(lineId);
        List<WmProductSalesDetail> details = wmProductSalesDetailMapper.selectWmProductSalesDetailList(param);
        BigDecimal remain = qualifiedQty;
        if (details != null) {
            for (WmProductSalesDetail d : details) {
                BigDecimal dq = d.getQuantity() == null ? BigDecimal.ZERO : d.getQuantity();
                if (remain.signum() <= 0) {
                    wmProductSalesDetailMapper.deleteWmProductSalesDetailByDetailId(d.getDetailId());
                    continue;
                }
                if (dq.compareTo(remain) > 0) {
                    WmProductSalesDetail du = new WmProductSalesDetail();
                    du.setDetailId(d.getDetailId());
                    du.setQuantity(remain);
                    du.setUpdateTime(DateUtils.getNowDate());
                    wmProductSalesDetailMapper.updateWmProductSalesDetail(du);
                    remain = BigDecimal.ZERO;
                } else {
                    remain = remain.subtract(dq);
                }
            }
        }
        if (remain.signum() > 0) {
            throw new ServiceException("拣货明细合计不足，无法缩减到合格量，请先补录拣货");
        }

        // ② 缩减出库行数量，并把不合格差额写入备注留痕
        BigDecimal diff = old.getQuantitySales() == null ? BigDecimal.ZERO
                : old.getQuantitySales().subtract(qualifiedQty);
        WmProductSalesLine update = new WmProductSalesLine();
        update.setLineId(lineId);
        update.setQuantitySales(qualifiedQty);
        if (diff.signum() > 0) {
            String trace = "出货检验合格量 " + qualifiedQty.stripTrailingZeros().toPlainString()
                    + "，出库数量由 " + old.getQuantitySales().stripTrailingZeros().toPlainString()
                    + " 缩减为 " + qualifiedQty.stripTrailingZeros().toPlainString()
                    + "，不合格差额 " + diff.stripTrailingZeros().toPlainString() + " 走退货/报废流程";
            update.setRemark(StringUtils.isEmpty(old.getRemark()) ? trace : old.getRemark() + "；" + trace);
        }
        update.setUpdateTime(DateUtils.getNowDate());
        return wmProductSalesLineMapper.updateWmProductSalesLine(update);
    }

    /**
     * 批量删除产品销售出库行
     * 
     * @param lineIds 需要删除的产品销售出库行主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmProductSalesLineByLineIds(Long[] lineIds)
    {
        if (lineIds != null) {
            for (Long lineId : lineIds) {
                WmProductSalesLine old = wmProductSalesLineMapper.selectWmProductSalesLineByLineId(lineId);
                if (old == null) {
                    continue;
                }
                requireEditableParent(old.getSalesId());
                if (old.getOqcId() != null) {
                    throw new ServiceException("出库行已生成出货检验，不能删除，请先作废检验单");
                }
            }
        }
        return wmProductSalesLineMapper.deleteWmProductSalesLineByLineIds(lineIds);
    }

    /**
     * 删除产品销售出库行信息
     * 
     * @param lineId 产品销售出库行主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmProductSalesLineByLineId(Long lineId)
    {
        WmProductSalesLine old = wmProductSalesLineMapper.selectWmProductSalesLineByLineId(lineId);
        if (old != null) {
            requireEditableParent(old.getSalesId());
            if (old.getOqcId() != null) {
                throw new ServiceException("出库行已生成出货检验，不能删除，请先作废检验单");
            }
        }
        return wmProductSalesLineMapper.deleteWmProductSalesLineByLineId(lineId);
    }

    @Override
    public int deleteBySalesId(Long salesId) {
        return wmProductSalesLineMapper.deleteBySalesId(salesId);
    }
}
