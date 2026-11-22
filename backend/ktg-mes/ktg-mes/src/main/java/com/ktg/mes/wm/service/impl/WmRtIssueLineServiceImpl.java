package com.ktg.mes.wm.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import org.springframework.transaction.annotation.Transactional;
import com.ktg.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ktg.mes.wm.mapper.WmRtIssueLineMapper;
import com.ktg.mes.wm.domain.WmRtIssueLine;
import com.ktg.mes.wm.service.IWmRtIssueLineService;

/**
 * 生产退料单行Service业务层处理
 * 
 * @author yinjinlu
 * @date 2022-09-15
 */
@Service
public class WmRtIssueLineServiceImpl implements IWmRtIssueLineService 
{
    @Autowired
    private WmRtIssueLineMapper wmRtIssueLineMapper;
    @Autowired
    private PostingLockMapper postingLockMapper;

    private void lockEditable(Long rtId) {
        if (rtId == null) throw new ServiceException("退料行必须关联退料单");
        String status = postingLockMapper.rtIssue(rtId);
        if (status == null || "FINISHED".equals(status) || "CANCELED".equals(status))
            throw new ServiceException("退料单不存在、已完成或已取消，不能修改退料行");
    }
    /** 普通行的增删只允许在草稿阶段进行；检验拆行/回写走专用方法。 */
    private void lockDraftOnly(Long rtId) {
        if (rtId == null) throw new ServiceException("退料行必须关联退料单");
        String status = postingLockMapper.rtIssue(rtId);
        if (!"PREPARE".equals(status))
            throw new ServiceException("只有草稿状态的退料单可以增删退料行");
    }
    private void assertNoInspection(WmRtIssueLine row) {
        if (postingLockMapper.rqcIdForRtIssueLine(row.getLineId()) != null)
            throw new ServiceException("退料行已关联检验单，不能删除；请先删除检验单");
        String quality = row.getQualityStatus();
        if (quality != null && !com.ktg.common.constant.UserConstants.QUALITY_STATUS_NOTTEST.equals(quality))
            throw new ServiceException("退料行已完成检验，不能删除");
    }
    private WmRtIssueLine existing(Long id) {
        WmRtIssueLine line = id == null ? null : wmRtIssueLineMapper.selectWmRtIssueLineByLineId(id);
        if (line == null || line.getRtId() == null) throw new ServiceException("退料行不存在或缺少退料单");
        return line;
    }
    private void assertCurrentOwner(WmRtIssueLine line) {
        if (!Objects.equals(line.getRtId(), postingLockMapper.rtIssueIdForLine(line.getLineId())))
            throw new ServiceException("退料行已变更，请刷新后重试");
    }

    /**
     * 查询生产退料单行
     * 
     * @param lineId 生产退料单行主键
     * @return 生产退料单行
     */
    @Override
    public WmRtIssueLine selectWmRtIssueLineByLineId(Long lineId)
    {
        return wmRtIssueLineMapper.selectWmRtIssueLineByLineId(lineId);
    }

    @Override
    public WmRtIssueLine selectWmRtIssueLineWithQuantityByLineId(Long lineId) {
        return wmRtIssueLineMapper.selectWmRtIssueLineWithQuantityByLineId(lineId);
    }

    /**
     * 查询生产退料单行列表
     * 
     * @param wmRtIssueLine 生产退料单行
     * @return 生产退料单行
     */
    @Override
    public List<WmRtIssueLine> selectWmRtIssueLineList(WmRtIssueLine wmRtIssueLine)
    {
        return wmRtIssueLineMapper.selectWmRtIssueLineList(wmRtIssueLine);
    }

    @Override
    public List<WmRtIssueLine> selectWmRtIssueLineWithQuantityList(WmRtIssueLine wmRtIssueLine) {
        return wmRtIssueLineMapper.selectWmRtIssueLineWithQuantityList(wmRtIssueLine);
    }

    @Override
    public List<WmRtIssueLine> selectWmRtIssueLineWithDetailList(WmRtIssueLine wmRtIssueLine) {
        return wmRtIssueLineMapper.selectWmRtIssueLineWithDetailList(wmRtIssueLine);
    }

    /**
     * 新增生产退料单行
     * 
     * @param wmRtIssueLine 生产退料单行
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWmRtIssueLine(WmRtIssueLine wmRtIssueLine)
    {
        lockDraftOnly(wmRtIssueLine.getRtId());
        // 需检标志由数据库/请求在创建时确定；需检行一律为“未检”，不接受请求伪造合格状态
        if (com.ktg.common.constant.UserConstants.YES.equals(wmRtIssueLine.getQcFlag())) {
            wmRtIssueLine.setQualityStatus(com.ktg.common.constant.UserConstants.QUALITY_STATUS_NOTTEST);
        }
        wmRtIssueLine.setCreateTime(DateUtils.getNowDate());
        return wmRtIssueLineMapper.insertWmRtIssueLine(wmRtIssueLine);
    }

    /**
     * 修改生产退料单行
     * 
     * @param wmRtIssueLine 生产退料单行
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWmRtIssueLine(WmRtIssueLine wmRtIssueLine)
    {
        WmRtIssueLine db = existing(wmRtIssueLine.getLineId());
        lockEditable(db.getRtId()); assertCurrentOwner(db);
        if (wmRtIssueLine.getRtId() != null && !Objects.equals(db.getRtId(), wmRtIssueLine.getRtId()))
            throw new ServiceException("不能修改退料行所属退料单");
        // 1) 需检标志以数据库为准，普通编辑不能改判
        boolean needInspect = com.ktg.common.constant.UserConstants.YES.equals(db.getQcFlag());
        wmRtIssueLine.setQcFlag(db.getQcFlag());
        // 2) 已检验或已有检验单的行：关键字段冻结，检验结论不被重算
        boolean inspected = !com.ktg.common.constant.UserConstants.QUALITY_STATUS_NOTTEST.equals(db.getQualityStatus());
        boolean hasRqc = postingLockMapper.rqcIdForRtIssueLine(db.getLineId()) != null;
        if (inspected || hasRqc) {
            if (changed(db.getItemId(), wmRtIssueLine.getItemId())
                    || changed(db.getBatchId(), wmRtIssueLine.getBatchId())
                    || changedText(db.getBatchCode(), wmRtIssueLine.getBatchCode())
                    || changedQuantity(db.getQuantityRt(), wmRtIssueLine.getQuantityRt()))
                throw new ServiceException(inspected
                        ? "该退料行已完成检验，物料、批次和数量不能修改；如需调整请取消检验单后重新发起"
                        : "该退料行已有检验单，物料、批次和数量不能修改；请先删除检验单");
            wmRtIssueLine.setQualityStatus(db.getQualityStatus());
        } else if (needInspect) {
            // 3) 未检验且需检：不允许在编辑时直接置为合格/不合格
            wmRtIssueLine.setQualityStatus(com.ktg.common.constant.UserConstants.QUALITY_STATUS_NOTTEST);
        }
        wmRtIssueLine.setUpdateTime(DateUtils.getNowDate());
        return wmRtIssueLineMapper.updateWmRtIssueLine(wmRtIssueLine);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int applyInspectionResult(WmRtIssueLine line) {
        WmRtIssueLine db = existing(line.getLineId());
        lockEditable(db.getRtId());
        assertCurrentOwner(db);
        line.setQcFlag(db.getQcFlag());
        line.setUpdateTime(DateUtils.getNowDate());
        return wmRtIssueLineMapper.updateWmRtIssueLine(line);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertInspectionSplit(WmRtIssueLine line) {
        lockEditable(line.getRtId());
        line.setCreateTime(DateUtils.getNowDate());
        return wmRtIssueLineMapper.insertWmRtIssueLine(line);
    }

    private boolean changed(Long dbValue, Long requestValue) {
        return requestValue != null && !Objects.equals(dbValue, requestValue);
    }
    private boolean changedText(String dbValue, String requestValue) {
        return requestValue != null && !requestValue.isEmpty() && !Objects.equals(dbValue, requestValue);
    }
    private boolean changedQuantity(java.math.BigDecimal dbValue, java.math.BigDecimal requestValue) {
        if (requestValue == null) return false;
        return dbValue == null || dbValue.compareTo(requestValue) != 0;
    }

    /**
     * 批量删除生产退料单行
     * 
     * @param lineIds 需要删除的生产退料单行主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmRtIssueLineByLineIds(Long[] lineIds)
    {
        TreeSet<Long> parents = new TreeSet<Long>();
        java.util.ArrayList<WmRtIssueLine> rows = new java.util.ArrayList<WmRtIssueLine>();
        for (Long id : lineIds) { WmRtIssueLine row = existing(id); rows.add(row); parents.add(row.getRtId()); }
        for (Long id : parents) lockDraftOnly(id);
        for (WmRtIssueLine row : rows) { assertCurrentOwner(row); assertNoInspection(row); }
        return wmRtIssueLineMapper.deleteWmRtIssueLineByLineIds(lineIds);
    }

    /**
     * 删除生产退料单行信息
     * 
     * @param lineId 生产退料单行主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmRtIssueLineByLineId(Long lineId)
    {
        WmRtIssueLine row = existing(lineId); lockDraftOnly(row.getRtId()); assertCurrentOwner(row); assertNoInspection(row);
        return wmRtIssueLineMapper.deleteWmRtIssueLineByLineId(lineId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByRtId(Long rtId) {
        lockEditable(rtId);
        return wmRtIssueLineMapper.deleteByRtId(rtId);
    }
}
