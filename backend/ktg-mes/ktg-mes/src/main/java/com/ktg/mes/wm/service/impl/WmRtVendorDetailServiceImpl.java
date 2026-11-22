package com.ktg.mes.wm.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.VendorReturnGuardMapper;
import com.ktg.mes.wm.mapper.WmRtVendorLineMapper;
import com.ktg.mes.wm.domain.WmRtVendorLine;
import org.springframework.transaction.annotation.Transactional;
import com.ktg.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ktg.mes.wm.mapper.WmRtVendorDetailMapper;
import com.ktg.mes.wm.domain.WmRtVendorDetail;
import com.ktg.mes.wm.service.IWmRtVendorDetailService;

/**
 * 采购退货单明细Service业务层处理
 * 
 * @author yinjinlu
 * @date 2025-03-03
 */
@Service
public class WmRtVendorDetailServiceImpl implements IWmRtVendorDetailService 
{
    @Autowired
    private WmRtVendorDetailMapper wmRtVendorDetailMapper;
    @Autowired private PostingLockMapper locks;
    @Autowired private VendorReturnGuardMapper guards;
    @Autowired private WmRtVendorLineMapper lineMapper;
    private void lockPicking(Long rtId) {
        if (rtId==null || !"APPROVING".equals(locks.rtVendor(rtId)))
            throw new ServiceException("只有待拣货退供单可以修改拣货明细");
        if ("PRE_STOCK".equals(guards.lockMode(rtId)))
            throw new ServiceException("未入库退供不能关联库存拣货明细");
    }
    private WmRtVendorDetail existing(Long id) {
        WmRtVendorDetail d=id==null?null:wmRtVendorDetailMapper.selectWmRtVendorDetailByDetailId(id);
        if(d==null || d.getRtId()==null)throw new ServiceException("退供拣货明细不存在");return d;
    }
    private void assertOwner(WmRtVendorDetail d) {
        if(!Objects.equals(d.getRtId(),locks.rtVendorIdForDetail(d.getDetailId())))
            throw new ServiceException("退供明细已变更，请刷新重试");
    }

    /**
     * 查询采购退货单明细
     * 
     * @param detailId 采购退货单明细主键
     * @return 采购退货单明细
     */
    @Override
    public WmRtVendorDetail selectWmRtVendorDetailByDetailId(Long detailId)
    {
        return wmRtVendorDetailMapper.selectWmRtVendorDetailByDetailId(detailId);
    }

    /**
     * 查询采购退货单明细列表
     * 
     * @param wmRtVendorDetail 采购退货单明细
     * @return 采购退货单明细
     */
    @Override
    public List<WmRtVendorDetail> selectWmRtVendorDetailList(WmRtVendorDetail wmRtVendorDetail)
    {
        return wmRtVendorDetailMapper.selectWmRtVendorDetailList(wmRtVendorDetail);
    }

    @Override
    public String checkQuantity(Long lineId) {
        return wmRtVendorDetailMapper.checkQuantity(lineId);
    }

    /**
     * 新增采购退货单明细
     * 
     * @param wmRtVendorDetail 采购退货单明细
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWmRtVendorDetail(WmRtVendorDetail wmRtVendorDetail)
    {
        lockPicking(wmRtVendorDetail.getRtId());
        WmRtVendorLine line=wmRtVendorDetail.getLineId()==null?null:lineMapper.selectWmRtVendorLineByLineId(wmRtVendorDetail.getLineId());
        if(line==null || !Objects.equals(line.getRtId(),wmRtVendorDetail.getRtId()))
            throw new ServiceException("拣货明细与退供行不属于同一退供单");
        wmRtVendorDetail.setCreateTime(DateUtils.getNowDate());
        return wmRtVendorDetailMapper.insertWmRtVendorDetail(wmRtVendorDetail);
    }

    /**
     * 修改采购退货单明细
     * 
     * @param wmRtVendorDetail 采购退货单明细
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWmRtVendorDetail(WmRtVendorDetail wmRtVendorDetail)
    {
        WmRtVendorDetail db=existing(wmRtVendorDetail.getDetailId());lockPicking(db.getRtId());assertOwner(db);
        if((wmRtVendorDetail.getRtId()!=null && !Objects.equals(db.getRtId(),wmRtVendorDetail.getRtId()))
                ||(wmRtVendorDetail.getLineId()!=null && !Objects.equals(db.getLineId(),wmRtVendorDetail.getLineId())))
            throw new ServiceException("不能修改拣货明细所属退供单或退供行");
        wmRtVendorDetail.setUpdateTime(DateUtils.getNowDate());
        return wmRtVendorDetailMapper.updateWmRtVendorDetail(wmRtVendorDetail);
    }

    /**
     * 批量删除采购退货单明细
     * 
     * @param detailIds 需要删除的采购退货单明细主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmRtVendorDetailByDetailIds(Long[] detailIds)
    {
        TreeSet<Long> parents=new TreeSet<Long>();java.util.ArrayList<WmRtVendorDetail> rows=new java.util.ArrayList<WmRtVendorDetail>();
        for(Long id:detailIds){WmRtVendorDetail row=existing(id);rows.add(row);parents.add(row.getRtId());}
        for(Long id:parents)lockPicking(id);for(WmRtVendorDetail row:rows)assertOwner(row);
        return wmRtVendorDetailMapper.deleteWmRtVendorDetailByDetailIds(detailIds);
    }

    /**
     * 删除采购退货单明细信息
     * 
     * @param detailId 采购退货单明细主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmRtVendorDetailByDetailId(Long detailId)
    {
        WmRtVendorDetail row=existing(detailId);lockPicking(row.getRtId());assertOwner(row);
        return wmRtVendorDetailMapper.deleteWmRtVendorDetailByDetailId(detailId);
    }
}
