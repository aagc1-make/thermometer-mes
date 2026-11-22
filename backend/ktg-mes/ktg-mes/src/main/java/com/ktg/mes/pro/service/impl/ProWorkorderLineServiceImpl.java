package com.ktg.mes.pro.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.BussinessException;
import com.ktg.common.utils.DateUtils;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.domain.ProWorkorderLine;
import com.ktg.mes.pro.mapper.ProWorkorderLineMapper;
import com.ktg.mes.pro.service.IProWorkorderLineService;

/**
 * 生产工单行 Service 实现
 *
 * @author ktg
 * @date 2026-11-05
 */
@Service
public class ProWorkorderLineServiceImpl implements IProWorkorderLineService
{
    @Autowired
    private ProWorkorderLineMapper proWorkorderLineMapper;

    @Override
    public ProWorkorderLine selectProWorkorderLineByLineId(Long lineId)
    {
        return proWorkorderLineMapper.selectProWorkorderLineByLineId(lineId);
    }

    @Override
    public List<ProWorkorderLine> selectProWorkorderLineList(ProWorkorderLine line)
    {
        return proWorkorderLineMapper.selectProWorkorderLineList(line);
    }

    @Override
    public int createDefaultLine(ProWorkorder workorder)
    {
        if (workorder == null || workorder.getWorkorderId() == null) {
            return 0;
        }
        // 幂等：工单已经有行时不动，避免重复建行
        if (proWorkorderLineMapper.countByWorkorderId(workorder.getWorkorderId()) > 0) {
            return 0;
        }
        // 没有产出物料无法确定行归属；后续写入工单行时会明确报错，而不是写 NULL。
        if (workorder.getProductId() == null || workorder.getProductId() == 0L) {
            return 0;
        }

        ProWorkorderLine line = new ProWorkorderLine();
        line.setWorkorderId(workorder.getWorkorderId());
        line.setLineNo(1);
        line.setItemId(workorder.getProductId());
        line.setItemCode(workorder.getProductCode());
        line.setItemName(workorder.getProductName());
        line.setItemSpc(workorder.getProductSpc());
        line.setUnitOfMeasure(workorder.getUnitOfMeasure());
        line.setUnitName(workorder.getUnitName());
        line.setPlanQuantity(workorder.getQuantity() == null ? BigDecimal.ZERO : workorder.getQuantity());
        line.setProducedQuantity(BigDecimal.ZERO);
        line.setQualifiedQuantity(BigDecimal.ZERO);
        line.setScrappedQuantity(BigDecimal.ZERO);
        line.setBatchStrategy("SINGLE");
        line.setStatus(mapLineStatus(workorder.getStatus()));
        line.setRemark("工单创建时自动生成（一工单一行项目）");
        line.setCreateTime(DateUtils.getNowDate());
        return proWorkorderLineMapper.insertProWorkorderLine(line);
    }

    @Override
    public Long resolveLineId(Long workorderId, Long requestedLineId, Long itemId)
    {
        if (workorderId == null) {
            throw new BussinessException("工单ID不能为空，无法确定工单行");
        }
        if (requestedLineId != null) {
            ProWorkorderLine line = proWorkorderLineMapper.selectProWorkorderLineByLineId(requestedLineId);
            if (line == null) {
                throw new BussinessException("工单行[" + requestedLineId + "]不存在，拒绝写入");
            }
            if (!workorderId.equals(line.getWorkorderId())) {
                throw new BussinessException("工单行[" + requestedLineId + "]不属于工单[" + workorderId + "]，拒绝写入");
            }
            return requestedLineId;
        }
        if (itemId == null) {
            throw new BussinessException("未指定工单行且物料为空，无法匹配工单行");
        }
        List<ProWorkorderLine> candidates =
                proWorkorderLineMapper.selectOpenLinesByWorkorderAndItem(workorderId, itemId);
        int count = candidates == null ? 0 : candidates.size();
        if (count == 1) {
            return candidates.get(0).getLineId();
        }
        if (count == 0) {
            throw new BussinessException("工单[" + workorderId + "]下未找到物料[" + itemId
                    + "]未关闭的工单行，请先为工单建立行项目");
        }
        throw new BussinessException("工单[" + workorderId + "]下物料[" + itemId + "]匹配到" + count
                + "条未关闭工单行，无法唯一判定，请显式指定 workorderLineId");
    }

    @Override
    public Long getDefaultLineId(Long workorderId)
    {
        if (workorderId == null) {
            return null;
        }
        ProWorkorderLine query = new ProWorkorderLine();
        query.setWorkorderId(workorderId);
        List<ProWorkorderLine> lines = proWorkorderLineMapper.selectProWorkorderLineList(query);
        return lines == null || lines.isEmpty() ? null : lines.get(0).getLineId();
    }

    /**
     * 工单状态映射到行状态（行表 CHECK 约束使用 PREPARE/IN_PROGRESS/FINISHED/CANCELLED）。
     */
    private String mapLineStatus(String workorderStatus)
    {
        if (UserConstants.ORDER_STATUS_FINISHED.equals(workorderStatus)) {
            return "FINISHED";
        }
        if (UserConstants.ORDER_STATUS_CANCELED.equals(workorderStatus)) {
            return "CANCELLED";
        }
        return "PREPARE";
    }
}
