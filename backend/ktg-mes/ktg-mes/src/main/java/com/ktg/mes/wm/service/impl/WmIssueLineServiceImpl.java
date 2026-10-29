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
import com.ktg.mes.wm.mapper.WmIssueLineMapper;
import com.ktg.mes.wm.domain.WmIssueLine;
import com.ktg.mes.wm.service.IWmIssueLineService;

/**
 * 生产领料单行Service业务层处理
 * 
 * @author yinjinlu
 * @date 2022-07-14
 */
@Service
public class WmIssueLineServiceImpl implements IWmIssueLineService 
{
    @Autowired
    private WmIssueLineMapper wmIssueLineMapper;
    @Autowired
    private PostingLockMapper postingLockMapper;

    private void lockEditable(Long issueId) {
        if (issueId == null) throw new ServiceException("领料行必须关联领料单");
        String status = postingLockMapper.issue(issueId);
        if (status == null || "FINISHED".equals(status) || "CANCELED".equals(status))
            throw new ServiceException("领料单不存在、已执行或已取消，不能修改领料行");
    }
    private WmIssueLine existing(Long id) {
        WmIssueLine line = id == null ? null : wmIssueLineMapper.selectWmIssueLineByLineId(id);
        if (line == null || line.getIssueId() == null) throw new ServiceException("领料行不存在或缺少领料单");
        return line;
    }
    private void assertCurrentOwner(WmIssueLine line) {
        if (!Objects.equals(line.getIssueId(), postingLockMapper.issueIdForLine(line.getLineId())))
            throw new ServiceException("领料行已被修改，请刷新后重试");
    }

    /**
     * 查询生产领料单行
     * 
     * @param lineId 生产领料单行主键
     * @return 生产领料单行
     */
    @Override
    public WmIssueLine selectWmIssueLineByLineId(Long lineId)
    {
        return wmIssueLineMapper.selectWmIssueLineByLineId(lineId);
    }

    /**
     * 查询生产领料单行列表
     * 
     * @param wmIssueLine 生产领料单行
     * @return 生产领料单行
     */
    @Override
    public List<WmIssueLine> selectWmIssueLineList(WmIssueLine wmIssueLine)
    {
        return wmIssueLineMapper.selectWmIssueLineList(wmIssueLine);
    }

    @Override
    public List<WmIssueLine> selectWmIssueLineWithOffShelfList(WmIssueLine wmIssueLine) {
        return wmIssueLineMapper.selectWmIssueLineWithOffShelfList(wmIssueLine);
    }

    @Override
    public List<WmIssueLine> selectWmIssueLineWithDetailList(WmIssueLine wmIssueLine) {
        return wmIssueLineMapper.selectWmIssueLineWithDetailList(wmIssueLine);
    }

    /**
     * 新增生产领料单行
     * 
     * @param wmIssueLine 生产领料单行
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWmIssueLine(WmIssueLine wmIssueLine)
    {
        lockEditable(wmIssueLine.getIssueId());
        wmIssueLine.setCreateTime(DateUtils.getNowDate());
        return wmIssueLineMapper.insertWmIssueLine(wmIssueLine);
    }

    /**
     * 修改生产领料单行
     * 
     * @param wmIssueLine 生产领料单行
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWmIssueLine(WmIssueLine wmIssueLine)
    {
        WmIssueLine db = existing(wmIssueLine.getLineId());
        lockEditable(db.getIssueId());
        assertCurrentOwner(db);
        if (wmIssueLine.getIssueId() != null && !Objects.equals(db.getIssueId(), wmIssueLine.getIssueId()))
            throw new ServiceException("不能修改领料行所属领料单");
        wmIssueLine.setUpdateTime(DateUtils.getNowDate());
        return wmIssueLineMapper.updateWmIssueLine(wmIssueLine);
    }

    /**
     * 批量删除生产领料单行
     * 
     * @param lineIds 需要删除的生产领料单行主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmIssueLineByLineIds(Long[] lineIds)
    {
        TreeSet<Long> parents = new TreeSet<Long>();
        java.util.ArrayList<WmIssueLine> lines = new java.util.ArrayList<WmIssueLine>();
        for (Long id : lineIds) { WmIssueLine line = existing(id); lines.add(line); parents.add(line.getIssueId()); }
        for (Long id : parents) lockEditable(id);
        for (WmIssueLine line : lines) assertCurrentOwner(line);
        return wmIssueLineMapper.deleteWmIssueLineByLineIds(lineIds);
    }

    /**
     * 删除生产领料单行信息
     * 
     * @param lineId 生产领料单行主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmIssueLineByLineId(Long lineId)
    {
        WmIssueLine line = existing(lineId);
        lockEditable(line.getIssueId());
        assertCurrentOwner(line);
        return wmIssueLineMapper.deleteWmIssueLineByLineId(lineId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIssueHeaderId(Long issueId) {
        lockEditable(issueId);
        return wmIssueLineMapper.deleteByIssueHeaderId(issueId);
    }
}
