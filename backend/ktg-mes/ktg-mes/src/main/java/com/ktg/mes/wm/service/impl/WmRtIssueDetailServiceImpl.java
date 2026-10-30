package com.ktg.mes.wm.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.WmRtIssueLineMapper;
import com.ktg.mes.wm.domain.WmRtIssueLine;
import org.springframework.transaction.annotation.Transactional;
import com.ktg.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ktg.mes.wm.mapper.WmRtIssueDetailMapper;
import com.ktg.mes.wm.domain.WmRtIssueDetail;
import com.ktg.mes.wm.service.IWmRtIssueDetailService;

/**
 * 生产退料单明细Service业务层处理
 * 
 * @author yinjinlu
 * @date 2025-03-05
 */
@Service
public class WmRtIssueDetailServiceImpl implements IWmRtIssueDetailService 
{
    @Autowired
    private WmRtIssueDetailMapper wmRtIssueDetailMapper;
    @Autowired
    private PostingLockMapper postingLockMapper;
    @Autowired
    private WmRtIssueLineMapper wmRtIssueLineMapper;

    private void lockEditable(Long rtId) {
        if (rtId == null) throw new ServiceException("退料明细必须关联退料单");
        String status = postingLockMapper.rtIssue(rtId);
        if (status == null || "FINISHED".equals(status) || "CANCELED".equals(status))
            throw new ServiceException("退料单不存在、已完成或已取消，不能修改退料明细");
    }
    private WmRtIssueDetail existing(Long id) {
        WmRtIssueDetail detail = id == null ? null : wmRtIssueDetailMapper.selectWmRtIssueDetailByDetailId(id);
        if (detail == null || detail.getRtId() == null) throw new ServiceException("退料明细不存在或缺少退料单");
        return detail;
    }
    private void assertCurrentOwner(WmRtIssueDetail detail) {
        if (!Objects.equals(detail.getRtId(), postingLockMapper.rtIssueIdForDetail(detail.getDetailId())))
            throw new ServiceException("退料明细已变更，请刷新后重试");
    }

    /**
     * 查询生产退料单明细
     * 
     * @param detailId 生产退料单明细主键
     * @return 生产退料单明细
     */
    @Override
    public WmRtIssueDetail selectWmRtIssueDetailByDetailId(Long detailId)
    {
        return wmRtIssueDetailMapper.selectWmRtIssueDetailByDetailId(detailId);
    }

    /**
     * 查询生产退料单明细列表
     * 
     * @param wmRtIssueDetail 生产退料单明细
     * @return 生产退料单明细
     */
    @Override
    public List<WmRtIssueDetail> selectWmRtIssueDetailList(WmRtIssueDetail wmRtIssueDetail)
    {
        return wmRtIssueDetailMapper.selectWmRtIssueDetailList(wmRtIssueDetail);
    }

    @Override
    public String checkQuantity(Long lineId) {
        return wmRtIssueDetailMapper.checkQuantity(lineId);
    }

    /**
     * 新增生产退料单明细
     * 
     * @param wmRtIssueDetail 生产退料单明细
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWmRtIssueDetail(WmRtIssueDetail wmRtIssueDetail)
    {
        lockEditable(wmRtIssueDetail.getRtId());
        WmRtIssueLine line = wmRtIssueDetail.getLineId() == null ? null : wmRtIssueLineMapper.selectWmRtIssueLineByLineId(wmRtIssueDetail.getLineId());
        if (line == null || !Objects.equals(line.getRtId(), wmRtIssueDetail.getRtId()))
            throw new ServiceException("退料明细与退料行不属于同一退料单");
        wmRtIssueDetail.setCreateTime(DateUtils.getNowDate());
        return wmRtIssueDetailMapper.insertWmRtIssueDetail(wmRtIssueDetail);
    }

    /**
     * 修改生产退料单明细
     * 
     * @param wmRtIssueDetail 生产退料单明细
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWmRtIssueDetail(WmRtIssueDetail wmRtIssueDetail)
    {
        WmRtIssueDetail db = existing(wmRtIssueDetail.getDetailId());
        lockEditable(db.getRtId()); assertCurrentOwner(db);
        if ((wmRtIssueDetail.getRtId() != null && !Objects.equals(db.getRtId(), wmRtIssueDetail.getRtId()))
                || (wmRtIssueDetail.getLineId() != null && !Objects.equals(db.getLineId(), wmRtIssueDetail.getLineId())))
            throw new ServiceException("不能修改退料明细所属退料单或退料行");
        wmRtIssueDetail.setUpdateTime(DateUtils.getNowDate());
        return wmRtIssueDetailMapper.updateWmRtIssueDetail(wmRtIssueDetail);
    }

    /**
     * 批量删除生产退料单明细
     * 
     * @param detailIds 需要删除的生产退料单明细主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmRtIssueDetailByDetailIds(Long[] detailIds)
    {
        TreeSet<Long> parents = new TreeSet<Long>();
        java.util.ArrayList<WmRtIssueDetail> rows = new java.util.ArrayList<WmRtIssueDetail>();
        for (Long id : detailIds) { WmRtIssueDetail row = existing(id); rows.add(row); parents.add(row.getRtId()); }
        for (Long id : parents) lockEditable(id);
        for (WmRtIssueDetail row : rows) assertCurrentOwner(row);
        return wmRtIssueDetailMapper.deleteWmRtIssueDetailByDetailIds(detailIds);
    }

    /**
     * 删除生产退料单明细信息
     * 
     * @param detailId 生产退料单明细主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmRtIssueDetailByDetailId(Long detailId)
    {
        WmRtIssueDetail row = existing(detailId); lockEditable(row.getRtId()); assertCurrentOwner(row);
        return wmRtIssueDetailMapper.deleteWmRtIssueDetailByDetailId(detailId);
    }
}
