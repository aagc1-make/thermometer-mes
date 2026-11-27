package com.ktg.mes.pro.service;

/**
 * 工单确认自动排产服务。
 *
 * @author ktg
 */
public interface IProAutoSchedulingService
{
    /**
     * 工单确认后按工艺路线一次性生成全部工序任务。
     *
     * <p>幂等：同一工单下相同工序已有任务时跳过，不会重复生成；
     * 任一工序缺路线/缺工作站时抛异常，由调用方事务整体回滚。</p>
     *
     * @param workorderId 生产工单ID
     * @param operator 操作人
     * @return 本次新生成的任务数量
     */
    int scheduleOnConfirm(Long workorderId, String operator);
}
