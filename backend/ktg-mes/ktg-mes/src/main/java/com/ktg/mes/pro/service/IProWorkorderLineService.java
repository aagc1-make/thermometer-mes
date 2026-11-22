package com.ktg.mes.pro.service;

import java.util.List;

import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.domain.ProWorkorderLine;

/**
 * 生产工单行 Service
 *
 * @author ktg
 * @date 2026-11-05
 */
public interface IProWorkorderLineService
{
    /**
     * 按行ID查询工单行
     */
    ProWorkorderLine selectProWorkorderLineByLineId(Long lineId);

    /**
     * 查询工单行列表
     */
    List<ProWorkorderLine> selectProWorkorderLineList(ProWorkorderLine line);

    /**
     * 为“一工单一行项目”的工单自动建一条默认行（幂等：已有行时直接返回）。
     *
     * @return 实际新建的行数
     */
    int createDefaultLine(ProWorkorder workorder);

    /**
     * 解析并校验工单行：
     * <ol>
     *   <li>显式传入 lineId 时，校验该行存在且属于当前工单；</li>
     *   <li>未传入时，按“同工单 + 同物料 + 未关闭”唯一匹配；</li>
     *   <li>匹配不到或匹配到多条时抛出业务异常，绝不静默写 NULL。</li>
     * </ol>
     *
     * @param workorderId     工单ID
     * @param requestedLineId 调用方显式传入的工单行ID，可为空
     * @param itemId          业务物料ID（报工/批次产出物料）
     * @return 校验通过的工单行ID（非空）
     */
    Long resolveLineId(Long workorderId, Long requestedLineId, Long itemId);

    /**
     * 取工单的默认行（line_no 最小的行），没有则返回 null。
     * 仅用于工单创建时派生 BOM 等行项目归属，不用于业务过账。
     */
    Long getDefaultLineId(Long workorderId);
}
