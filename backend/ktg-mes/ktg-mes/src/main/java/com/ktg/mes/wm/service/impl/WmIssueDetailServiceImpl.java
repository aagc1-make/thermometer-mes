package com.ktg.mes.wm.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.WmIssueLineMapper;
import com.ktg.mes.wm.domain.WmIssueLine;
import org.springframework.transaction.annotation.Transactional;
import com.ktg.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ktg.mes.wm.mapper.WmIssueDetailMapper;
import com.ktg.mes.wm.domain.WmIssueDetail;
import com.ktg.mes.wm.service.IWmIssueDetailService;

/**
 * 生产领料单明细Service业务层处理
 * 
 * @author yinjinlu
 * @date 2025-03-04
 */
@Service
public class WmIssueDetailServiceImpl implements IWmIssueDetailService 
{
    @Autowired
    private WmIssueDetailMapper wmIssueDetailMapper;
    @Autowired
    private PostingLockMapper postingLockMapper;
    @Autowired
    private WmIssueLineMapper wmIssueLineMapper;

    private void lockEditable(Long issueId) {
        if (issueId == null) throw new ServiceException("拣货明细必须关联领料单");
        String status = postingLockMapper.issue(issueId);
        if (status == null || "FINISHED".equals(status) || "CANCELED".equals(status))
            throw new ServiceException("领料单不存在、已执行或已取消，不能修改拣货明细");
    }

    private WmIssueDetail existing(Long id) {
        WmIssueDetail d = id == null ? null : wmIssueDetailMapper.selectWmIssueDetailByDetailId(id);
        if (d == null || d.getIssueId() == null) throw new ServiceException("拣货明细不存在或缺少领料单");
        return d;
    }

    private void assertCurrentOwner(WmIssueDetail d) {
        if (!Objects.equals(d.getIssueId(), postingLockMapper.issueIdForDetail(d.getDetailId())))
            throw new ServiceException("拣货明细已被修改，请刷新后重试");
    }

    /**
     * 查询生产领料单明细
     * 
     * @param detailId 生产领料单明细主键
     * @return 生产领料单明细
     */
    @Override
    public WmIssueDetail selectWmIssueDetailByDetailId(Long detailId)
    {
        return wmIssueDetailMapper.selectWmIssueDetailByDetailId(detailId);
    }

    /**
     * 查询生产领料单明细列表
     * 
     * @param wmIssueDetail 生产领料单明细
     * @return 生产领料单明细
     */
    @Override
    public List<WmIssueDetail> selectWmIssueDetailList(WmIssueDetail wmIssueDetail)
    {
        return wmIssueDetailMapper.selectWmIssueDetailList(wmIssueDetail);
    }

    @Override
    public String checkQuantity(Long lineId) {
        return wmIssueDetailMapper.checkQuantity(lineId);
    }

    /**
     * 新增生产领料单明细
     * 
     * @param wmIssueDetail 生产领料单明细
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWmIssueDetail(WmIssueDetail wmIssueDetail)
    {
        lockEditable(wmIssueDetail.getIssueId());
        WmIssueLine line = wmIssueDetail.getLineId() == null ? null : wmIssueLineMapper.selectWmIssueLineByLineId(wmIssueDetail.getLineId());
        if (line == null || !Objects.equals(line.getIssueId(), wmIssueDetail.getIssueId()))
            throw new ServiceException("拣货明细与领料行不属于同一领料单");
        wmIssueDetail.setCreateTime(DateUtils.getNowDate());
        return wmIssueDetailMapper.insertWmIssueDetail(wmIssueDetail);
    }

    /**
     * 修改生产领料单明细
     * 
     * @param wmIssueDetail 生产领料单明细
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWmIssueDetail(WmIssueDetail wmIssueDetail)
    {
        WmIssueDetail db = existing(wmIssueDetail.getDetailId());
        lockEditable(db.getIssueId());
        assertCurrentOwner(db);
        if ((wmIssueDetail.getIssueId() != null && !Objects.equals(db.getIssueId(), wmIssueDetail.getIssueId()))
                || (wmIssueDetail.getLineId() != null && !Objects.equals(db.getLineId(), wmIssueDetail.getLineId())))
            throw new ServiceException("不能修改拣货明细的领料单或领料行");
        wmIssueDetail.setUpdateTime(DateUtils.getNowDate());
        return wmIssueDetailMapper.updateWmIssueDetail(wmIssueDetail);
    }

    /**
     * 批量删除生产领料单明细
     * 
     * @param detailIds 需要删除的生产领料单明细主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmIssueDetailByDetailIds(Long[] detailIds)
    {
        TreeSet<Long> parents = new TreeSet<Long>();
        java.util.ArrayList<WmIssueDetail> details = new java.util.ArrayList<WmIssueDetail>();
        for (Long id : detailIds) { WmIssueDetail d = existing(id); details.add(d); parents.add(d.getIssueId()); }
        for (Long id : parents) lockEditable(id);
        for (WmIssueDetail d : details) assertCurrentOwner(d);
        return wmIssueDetailMapper.deleteWmIssueDetailByDetailIds(detailIds);
    }

    /**
     * 删除生产领料单明细信息
     * 
     * @param detailId 生产领料单明细主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmIssueDetailByDetailId(Long detailId)
    {
        WmIssueDetail d = existing(detailId);
        lockEditable(d.getIssueId());
        assertCurrentOwner(d);
        return wmIssueDetailMapper.deleteWmIssueDetailByDetailId(detailId);
    }
}
