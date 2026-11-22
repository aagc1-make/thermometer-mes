package com.ktg.mes.pro.service.impl;

import java.math.BigDecimal;
import java.util.List;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.utils.DateUtils;
import com.ktg.mes.pro.domain.ProRouteProcess;
import com.ktg.mes.pro.domain.ProTask;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.mapper.ProRouteProcessMapper;
import com.ktg.mes.pro.mapper.ProTaskMapper;
import com.ktg.mes.pro.mapper.ProWorkorderMapper;
import com.ktg.mes.pro.service.IProTaskService;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.domain.WmItemConsumeLine;
import com.ktg.mes.wm.domain.WmProductProduceLine;
import com.ktg.mes.wm.mapper.WmProductProduceLineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.ktg.mes.pro.mapper.ProFeedbackMapper;
import com.ktg.mes.pro.domain.ProFeedback;
import com.ktg.mes.pro.service.IProFeedbackService;
import org.springframework.util.CollectionUtils;

/**
 * 生产报工记录Service业务层处理
 * 
 * @author yinjinlu
 * @date 2022-07-10
 */
@Service
public class ProFeedbackServiceImpl implements IProFeedbackService 
{
    @Autowired
    private ProFeedbackMapper proFeedbackMapper;

    @Autowired
    private ProTaskMapper proTaskMapper;

    @Autowired
    private ProWorkorderMapper proWorkorderMapper;

    @Autowired
    private ProRouteProcessMapper proRouteProcessMapper;

    @Autowired
    private WmProductProduceLineMapper wmProductProduceLineMapper;


    /**
     * 查询生产报工记录
     * 
     * @param recordId 生产报工记录主键
     * @return 生产报工记录
     */
    @Override
    public ProFeedback selectProFeedbackByRecordId(Long recordId)
    {
        return proFeedbackMapper.selectProFeedbackByRecordId(recordId);
    }

    /**
     * 查询生产报工记录列表
     * 
     * @param proFeedback 生产报工记录
     * @return 生产报工记录
     */
    @Override
    public List<ProFeedback> selectProFeedbackList(ProFeedback proFeedback)
    {
        return proFeedbackMapper.selectProFeedbackList(proFeedback);
    }

    /**
     * 新增生产报工记录
     * 
     * @param proFeedback 生产报工记录
     * @return 结果
     */
    @Override
    public int insertProFeedback(ProFeedback proFeedback)
    {
        proFeedback.setCreateTime(DateUtils.getNowDate());
        return proFeedbackMapper.insertProFeedback(proFeedback);
    }

    /**
     * 报工单号统一在服务端生成，避免前端预取的长单号落库。
     * 并发下同工单可能算出同一序号，撞唯一索引后重新取号重试。
     */
    @Override
    public int insertProFeedbackWithCode(ProFeedback proFeedback, String workorderCode)
    {
        for (int attempt = 0; attempt < 3; attempt++)
        {
            proFeedback.setFeedbackCode(generateFeedbackCode(workorderCode, proFeedback.getWorkorderId()));
            try
            {
                return insertProFeedback(proFeedback);
            }
            catch (DuplicateKeyException e)
            {
                if (attempt == 2)
                {
                    throw e;
                }
            }
        }
        return 0;
    }

    @Override
    public String generateFeedbackCode(String workorderCode, Long workorderId)
    {
        String prefix = workorderCode == null ? "" : workorderCode.trim();
        int next = 1;
        if (workorderId != null)
        {
            ProFeedback query = new ProFeedback();
            query.setWorkorderId(workorderId);
            List<ProFeedback> rows = proFeedbackMapper.selectProFeedbackList(query);
            if (rows != null)
            {
                for (ProFeedback row : rows)
                {
                    Integer serial = parseFeedbackSerial(prefix, row.getFeedbackCode());
                    if (serial != null && serial >= next)
                    {
                        next = serial + 1;
                    }
                }
            }
        }
        return prefix + "-bg" + String.format("%03d", next);
    }

    /** 解析「父工单号-bg序号」中的序号；兼容旧的「父工单号-bg日期+5位流水」。 */
    private Integer parseFeedbackSerial(String workorderCode, String feedbackCode)
    {
        String prefix = workorderCode + "-bg";
        if (feedbackCode == null || !feedbackCode.startsWith(prefix))
        {
            return null;
        }
        String tail = feedbackCode.substring(prefix.length());
        if (tail.isEmpty() || !tail.chars().allMatch(Character::isDigit))
        {
            return null;
        }
        // 旧格式尾部是 yyyyMMdd+5位流水，取后 5 位再按数值处理
        if (tail.length() > 5)
        {
            tail = tail.substring(tail.length() - 5);
        }
        try
        {
            return Integer.valueOf(tail);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * 修改生产报工记录
     * 
     * @param proFeedback 生产报工记录
     * @return 结果
     */
    @Override
    public int updateProFeedback(ProFeedback proFeedback)
    {
        proFeedback.setUpdateTime(DateUtils.getNowDate());
        return proFeedbackMapper.updateProFeedback(proFeedback);
    }

    /**
     * 根据当前报工单的最终结果更新生产任务和生产工单的进度
     * @param feedback
     * 使用当前报工单的报工数量，更新任务的已生产数量
     * 使用当前报工单的报工数量，更新工单的已完成数量
     * 使用当前报工单关联的产品产出单上的合格品、不合格品数量更新任务的合格品、不合格品数量
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void updateProTaskAndWorkorderByFeedback(ProFeedback feedback) {

        BigDecimal quantityQuanlify = BigDecimal.ZERO;
        BigDecimal quantityUnquanlify = BigDecimal.ZERO;
        WmProductProduceLine param = new WmProductProduceLine();
        param.setFeedbackId(feedback.getRecordId());
        List<WmProductProduceLine> lines = wmProductProduceLineMapper.selectWmProductProduceLineList(param);
        if(!CollectionUtils.isEmpty(lines)){
            for(WmProductProduceLine line:lines){
                if(UserConstants.QUALITY_STATUS_NOTGOOD.equals(line.getQualityStatus())){
                    quantityUnquanlify = quantityUnquanlify.add(line.getQuantityProduce());
                }

                if(UserConstants.QUALITY_STATUS_PASS.equals(line.getQualityStatus())){
                    quantityQuanlify = quantityQuanlify.add(line.getQuantityProduce());
                }
            }
        }

        if (CollectionUtils.isEmpty(lines)) {
            // 非关键工序没有产品产出行，直接按报工单自身的合格/不良数量推进任务，避免合格数一直为 0
            quantityQuanlify = feedback.getQuantityQualified() == null ? BigDecimal.ZERO : feedback.getQuantityQualified();
            quantityUnquanlify = feedback.getQuantityUnquanlified() == null ? BigDecimal.ZERO : feedback.getQuantityUnquanlified();
        }

        BigDecimal produced = feedback.getQuantityFeedback() == null ? BigDecimal.ZERO : feedback.getQuantityFeedback();
        proTaskMapper.incrementProduced(feedback.getTaskId(), produced, quantityQuanlify, quantityUnquanlify);
        // 工单“累计报工”只认末道工序产出，避免五道工序各报 10 被累加成 50
        if (isTerminalProcess(feedback)) {
            proWorkorderMapper.incrementProduced(feedback.getWorkorderId(), produced);
        }

    }

    /** 当前报工工序是否为工艺路线末道工序（next_process_id = 0）。 */
    private boolean isTerminalProcess(ProFeedback feedback) {
        if (feedback.getRouteId() == null || feedback.getProcessId() == null) {
            return false;
        }
        ProRouteProcess query = new ProRouteProcess();
        query.setRouteId(feedback.getRouteId());
        query.setNextProcessId(0L);
        List<ProRouteProcess> terminals = proRouteProcessMapper.selectProRouteProcessList(query);
        if (CollectionUtils.isEmpty(terminals)) {
            return false;
        }
        return terminals.stream().anyMatch(row -> feedback.getProcessId().equals(row.getProcessId()));
    }

    /**
     * 批量删除生产报工记录
     * 
     * @param recordIds 需要删除的生产报工记录主键
     * @return 结果
     */
    @Override
    public int deleteProFeedbackByRecordIds(Long[] recordIds)
    {
        return proFeedbackMapper.deleteProFeedbackByRecordIds(recordIds);
    }

    /**
     * 删除生产报工记录信息
     * 
     * @param recordId 生产报工记录主键
     * @return 结果
     */
    @Override
    public int deleteProFeedbackByRecordId(Long recordId)
    {
        return proFeedbackMapper.deleteProFeedbackByRecordId(recordId);
    }

    @Override
    public List<ProFeedback> selectByWorkorderIds(List<Long> workorderIds) {
        return proFeedbackMapper.selectByWorkorderIds(workorderIds);
    }
}
