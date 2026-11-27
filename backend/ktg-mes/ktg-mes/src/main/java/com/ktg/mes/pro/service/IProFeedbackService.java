package com.ktg.mes.pro.service;

import java.util.List;
import com.ktg.mes.pro.domain.ProFeedback;

/**
 * 生产报工记录Service接口
 * 
 * @author yinjinlu
 * @date 2022-07-10
 */
public interface IProFeedbackService 
{
    /**
     * 查询生产报工记录
     * 
     * @param recordId 生产报工记录主键
     * @return 生产报工记录
     */
    public ProFeedback selectProFeedbackByRecordId(Long recordId);

    /**
     * 查询生产报工记录列表
     * 
     * @param proFeedback 生产报工记录
     * @return 生产报工记录集合
     */
    public List<ProFeedback> selectProFeedbackList(ProFeedback proFeedback);

    /**
     * 新增生产报工记录
     * 
     * @param proFeedback 生产报工记录
     * @return 结果
     */
    public int insertProFeedback(ProFeedback proFeedback);

    /**
     * 按「父工单号-bg三位序号」生成报工单号并保存（如 gd2026112100003-bg012）。
     * 同一工单内序号递增，撞号时自动重新取号重试。
     */
    public int insertProFeedbackWithCode(ProFeedback proFeedback, String workorderCode);

    /**
     * 生成报工单号：父工单号-bg三位序号，序号取该工单已有报工的最大值 +1。
     */
    public String generateFeedbackCode(String workorderCode, Long workorderId);

    /**
     * 修改生产报工记录
     * 
     * @param proFeedback 生产报工记录
     * @return 结果
     */
    public int updateProFeedback(ProFeedback proFeedback);

    /**
     * 根据当前报工单的最终结果更新生产任务和生产工单的进度
     * @param proFeedback
     */
    public void updateProTaskAndWorkorderByFeedback(ProFeedback proFeedback);


    /**
     * 批量删除生产报工记录
     * 
     * @param recordIds 需要删除的生产报工记录主键集合
     * @return 结果
     */
    public int deleteProFeedbackByRecordIds(Long[] recordIds);

    /**
     * 删除生产报工记录信息
     * 
     * @param recordId 生产报工记录主键
     * @return 结果
     */
    public int deleteProFeedbackByRecordId(Long recordId);

    /**
     * 根据 workorderIds 查询
     * @param workorderIds
     * @return
     */
    List<ProFeedback> selectByWorkorderIds(List<Long> workorderIds);
}
