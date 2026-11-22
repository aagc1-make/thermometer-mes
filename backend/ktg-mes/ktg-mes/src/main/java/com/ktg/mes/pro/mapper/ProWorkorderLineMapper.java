package com.ktg.mes.pro.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ktg.mes.pro.domain.ProWorkorderLine;

/**
 * 生产工单行 Mapper
 *
 * @author ktg
 * @date 2026-11-05
 */
public interface ProWorkorderLineMapper
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
     * 查询同一工单下指定物料、未关闭的工单行（FINISHED / CANCELLED 视为已关闭）
     */
    List<ProWorkorderLine> selectOpenLinesByWorkorderAndItem(@Param("workorderId") Long workorderId,
                                                             @Param("itemId") Long itemId);

    /**
     * 当前工单已有的行数
     */
    int countByWorkorderId(Long workorderId);

    /**
     * 新增工单行
     */
    int insertProWorkorderLine(ProWorkorderLine line);

    /**
     * 按工单ID批量删除工单行
     */
    int deleteProWorkorderLineByWorkorderIds(Long[] workorderIds);
}
