package com.ktg.mes.qc.service.impl;

import java.math.BigDecimal;
import java.util.List;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.DateUtils;
import com.ktg.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ktg.mes.qc.mapper.QcOqcMapper;
import com.ktg.mes.qc.domain.QcOqc;
import com.ktg.mes.qc.service.IQcOqcService;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import com.ktg.mes.wm.service.IWmProductSalesLineService;
import com.ktg.mes.wm.service.IWmProductSalesService;

/**
 * 出货检验单Service业务层处理
 * 
 * @author yinjinlu
 * @date 2022-08-31
 */
@Service
public class QcOqcServiceImpl implements IQcOqcService 
{
    @Autowired
    private QcOqcMapper qcOqcMapper;

    @Autowired
    private IWmProductSalesLineService wmProductSalesLineService;

    @Autowired
    private IWmProductSalesService wmProductSalesService;

    /**
     * 查询出货检验单
     * 
     * @param oqcId 出货检验单主键
     * @return 出货检验单
     */
    @Override
    public QcOqc selectQcOqcByOqcId(Long oqcId)
    {
        return qcOqcMapper.selectQcOqcByOqcId(oqcId);
    }

    /**
     * 查询出货检验单列表
     * 
     * @param qcOqc 出货检验单
     * @return 出货检验单
     */
    @Override
    public List<QcOqc> selectQcOqcList(QcOqc qcOqc)
    {
        return qcOqcMapper.selectQcOqcList(qcOqc);
    }

    @Override
    public String checkOqcCodeUnique(QcOqc qcOqc) {
        QcOqc oqc = qcOqcMapper.checkOqcCodeUnique(qcOqc);
        Long oqcId = qcOqc.getOqcId() == null? -1L : qcOqc.getOqcId();
        if(StringUtils.isNotNull(oqc) && oqc.getOqcId().longValue() != oqcId.longValue()){
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 新增出货检验单
     * 
     * @param qcOqc 出货检验单
     * @return 结果
     */
    @Override
    public int insertQcOqc(QcOqc qcOqc)
    {
        qcOqc.setCreateTime(DateUtils.getNowDate());
        return qcOqcMapper.insertQcOqc(qcOqc);
    }

    /**
     * 修改出货检验单
     * 
     * @param qcOqc 出货检验单
     * @return 结果
     */
    @Override
    public int updateQcOqc(QcOqc qcOqc)
    {
        qcOqc.setUpdateTime(DateUtils.getNowDate());
        return qcOqcMapper.updateQcOqc(qcOqc);
    }

    /**
     * 带旧状态条件的完成（并发只允许一个成功）
     */
    @Override
    public int finishOqcIfPending(Long oqcId)
    {
        return qcOqcMapper.finishOqcIfPending(oqcId);
    }

    /**
     * 出货检验完成的统一事务编排（阶段1第4轮复审修复）
     *
     * 顺序：条件更新抢占 → 回写销售行质量结果 → 汇总销售主单状态 → 更新检验单。
     * 任一步失败抛 ServiceException，整体回滚，不会留下"已完成但未回写"的单据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOqc(QcOqc qcOqc)
    {
        if (qcOqc == null || qcOqc.getOqcId() == null) {
            throw new ServiceException("出货检验单ID不能为空");
        }

        /* ===== 阶段1第5轮复审：先做全部校验，再写入 ===== */

        // ① 从数据库恢复既有检验单，来源以库中记录为准（防止请求篡改或省略来源）
        QcOqc dbOqc = qcOqcMapper.selectQcOqcByOqcId(qcOqc.getOqcId());
        if (dbOqc == null) {
            throw new ServiceException("出货检验单不存在");
        }
        if (qcOqc.getSourceDocId() != null
                && !qcOqc.getSourceDocId().equals(dbOqc.getSourceDocId())) {
            throw new ServiceException("不能修改出货检验单的来源单据");
        }
        if (qcOqc.getSourceLineId() != null
                && !qcOqc.getSourceLineId().equals(dbOqc.getSourceLineId())) {
            throw new ServiceException("不能修改出货检验单的来源行");
        }
        Long salesId = dbOqc.getSourceDocId();
        Long lineId = dbOqc.getSourceLineId();
        if (salesId == null || lineId == null) {
            throw new ServiceException("检验单缺少来源单据/来源行，无法完成");
        }
        qcOqc.setSourceDocId(salesId);
        qcOqc.setSourceLineId(lineId);

        // ② 来源归属校验：行必须属于该销售单，物料与批次必须与行一致
        WmProductSalesLine line = wmProductSalesLineService
                .selectWmProductSalesLineByLineId(lineId);
        if (line == null) {
            throw new ServiceException("来源出库行不存在，无法回写检验结果");
        }
        if (line.getSalesId() != null && !line.getSalesId().equals(salesId)) {
            throw new ServiceException("来源行与来源销售单不匹配");
        }
        Long reqItemId = qcOqc.getItemId() != null ? qcOqc.getItemId() : dbOqc.getItemId();
        if (reqItemId != null && line.getItemId() != null && !reqItemId.equals(line.getItemId())) {
            throw new ServiceException("检验物料与来源行物料不一致");
        }
        String reqBatch = StringUtils.isNotEmpty(qcOqc.getBatchCode())
                ? qcOqc.getBatchCode() : dbOqc.getBatchCode();
        if (StringUtils.isNotEmpty(reqBatch) && StringUtils.isNotEmpty(line.getBatchCode())
                && !reqBatch.equals(line.getBatchCode())) {
            throw new ServiceException("检验批次与来源行批次不一致");
        }

        // ③ 数量守恒校验：非负、合格+不合格=检测量、不超来源可处理量
        BigDecimal checkQty = qcOqc.getQuantityCheck() != null
                ? qcOqc.getQuantityCheck() : dbOqc.getQuantityCheck();
        BigDecimal qualified = qcOqc.getQuantityQualified() != null
                ? qcOqc.getQuantityQualified() : dbOqc.getQuantityQualified();
        BigDecimal unqualified = qcOqc.getQuantityUnqualified() != null
                ? qcOqc.getQuantityUnqualified() : dbOqc.getQuantityUnqualified();
        if (checkQty == null || checkQty.signum() <= 0) {
            throw new ServiceException("检测数量必须大于零，不能以 0 检测量放行");
        }
        if (qualified == null || qualified.signum() < 0
                || unqualified == null || unqualified.signum() < 0) {
            throw new ServiceException("合格/不合格数量不能为空或负数");
        }
        if (qualified.add(unqualified).compareTo(checkQty) != 0) {
            throw new ServiceException("合格数与不合格数之和必须等于检测数量");
        }
        if (line.getQuantitySales() != null && checkQty.compareTo(line.getQuantitySales()) > 0) {
            throw new ServiceException("检测数量超过来源出库行可处理数量");
        }

        // ③-b 放行量保护（第6/7轮复审）：
        //   - "合格为 0 不得放行"只作用于合格放行分支；全 NG 判定必须能完成并阻断销售；
        //   - 合格量小于出库数量时，出库数量必须缩减到合格量（禁止超检验合格量执行）。
        boolean acceptResult = UserConstants.QUALITY_CHECK_RESULT_ACCEPT.equals(qcOqc.getCheckResult());
        if (qualified.signum() == 0 && acceptResult) {
            throw new ServiceException("合格数量为 0，不能按合格放行；请判定为拒收");
        }
        if (acceptResult && line.getQuantitySales() != null
                && qualified.compareTo(line.getQuantitySales()) < 0) {
            wmProductSalesLineService.applyQualifiedQuantity(line.getLineId(), qualified);
        }

        // ③-c 来源主单阶段校验（第6轮复审）：已执行/已取消的销售单不允许被遗留草稿改写
        WmProductSales salesForCheck = wmProductSalesService.selectWmProductSalesBySalesId(salesId);
        if (salesForCheck == null) {
            throw new ServiceException("来源销售单不存在，无法完成检验");
        }
        String salesStatus = salesForCheck.getStatus();
        if (UserConstants.ORDER_STATUS_FINISHED.equals(salesStatus)
                || UserConstants.PRODUCT_SALES_STATUS_CANCELED.equals(salesStatus)) {
            throw new ServiceException("来源销售单已执行或已取消，不能再完成该出货检验");
        }
        if (!UserConstants.PRODUCT_SALES_STATUS_UNCHECK.equals(salesStatus)) {
            throw new ServiceException("来源销售单当前不在待检状态，无法完成出货检验");
        }
        // 该出库行已经被别的检验单完成过时，不允许覆盖
        if (line.getOqcId() != null && !line.getOqcId().equals(qcOqc.getOqcId())) {
            throw new ServiceException("该出库行已由其他出货检验单完成，不能重复回写");
        }

        // ④ 并发抢占：只有第一个请求能把状态从非 FINISHED 改为 FINISHED
        if (qcOqcMapper.finishOqcIfPending(qcOqc.getOqcId()) == 0) {
            throw new ServiceException("该出货检验单已被其他请求完成，请刷新后查看");
        }

        // ⑤ 回写销售行：合格判定以本次检验结果为准，不复制原值
        {
            WmProductSalesLine writeBack = new WmProductSalesLine();
            writeBack.setLineId(line.getLineId());
            writeBack.setOqcId(qcOqc.getOqcId());
            writeBack.setOqcCode(qcOqc.getOqcCode());
            writeBack.setQualityStatus(
                    UserConstants.QUALITY_CHECK_RESULT_ACCEPT.equals(qcOqc.getCheckResult())
                            ? UserConstants.QUALITY_STATUS_PASS
                            : UserConstants.QUALITY_STATUS_NOTGOOD);
            wmProductSalesLineService.writeBackOqcResult(writeBack);

            // ⑥ 汇总销售主单：任一 NG→CANCELED；全部需检行明确合格才→UNSHIPPING
            WmProductSalesLine param = new WmProductSalesLine();
            param.setSalesId(salesId);
            List<WmProductSalesLine> lines = wmProductSalesLineService.selectWmProductSalesLineList(param);
            boolean anyNg = false;
            boolean anyPending = false;
            for (WmProductSalesLine l : lines) {
                if (UserConstants.QUALITY_STATUS_NOTGOOD.equals(l.getQualityStatus())) {
                    anyNg = true;
                } else if (UserConstants.YES.equals(l.getOqcCheck())
                        && !UserConstants.QUALITY_STATUS_PASS.equals(l.getQualityStatus())) {
                    // 只把"明确合格(PASS)"视为完成；空值、NT、其他中间态都算待检
                    anyPending = true;
                }
            }
            WmProductSales sales = wmProductSalesService.selectWmProductSalesBySalesId(salesId);
            if (sales == null) {
                throw new ServiceException("来源销售单不存在，无法汇总状态");
            }
            if (anyNg) {
                sales.setStatus(UserConstants.ORDER_STATUS_CANCELED);
                wmProductSalesService.updateWmProductSales(sales);
            } else if (!anyPending && !UserConstants.PRODUCT_SALES_STATUS_UNSHIPPING.equals(sales.getStatus())) {
                sales.setStatus(UserConstants.PRODUCT_SALES_STATUS_UNSHIPPING);
                wmProductSalesService.updateWmProductSales(sales);
            }
        }

        // ⑦ 更新检验单本体（状态已由条件更新置为 FINISHED，这里补齐结果等字段）
        qcOqc.setStatus(UserConstants.ORDER_STATUS_FINISHED);
        qcOqc.setUpdateTime(DateUtils.getNowDate());
        qcOqcMapper.updateQcOqc(qcOqc);
    }

    @Override
    public int updateCrMajMinQuaAndRate(Long oqcId) {
        return qcOqcMapper.updateCrMajMinQuaAndRate(oqcId);
    }

    /**
     * 批量删除出货检验单
     * 
     * @param oqcIds 需要删除的出货检验单主键
     * @return 结果
     */
    @Override
    public int deleteQcOqcByOqcIds(Long[] oqcIds)
    {
        return qcOqcMapper.deleteQcOqcByOqcIds(oqcIds);
    }

    /**
     * 删除出货检验单信息
     * 
     * @param oqcId 出货检验单主键
     * @return 结果
     */
    @Override
    public int deleteQcOqcByOqcId(Long oqcId)
    {
        return qcOqcMapper.deleteQcOqcByOqcId(oqcId);
    }
}
