package com.ktg.mes.wm.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.VendorReturnGuardMapper;
import com.ktg.mes.wm.mapper.WmRtVendorDetailMapper;
import com.ktg.mes.wm.domain.WmRtVendorDetail;
import org.springframework.transaction.annotation.Transactional;
import com.ktg.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ktg.mes.wm.mapper.WmRtVendorLineMapper;
import com.ktg.mes.wm.domain.WmRtVendorLine;
import com.ktg.mes.wm.service.IWmRtVendorLineService;

/**
 * 供应商退货行Service业务层处理
 * 
 * @author yinjinlu
 * @date 2022-06-13
 */
@Service
public class WmRtVendorLineServiceImpl implements IWmRtVendorLineService 
{
    @Autowired
    private WmRtVendorLineMapper wmRtVendorLineMapper;
    @Autowired private WmRtVendorDetailMapper wmRtVendorDetailMapper;
    @Autowired private PostingLockMapper locks;
    @Autowired private VendorReturnGuardMapper guards;
    private String lockDraft(Long rtId) {
        if (rtId==null || !"PREPARE".equals(locks.rtVendor(rtId)))
            throw new ServiceException("只有退供草稿可以修改退供行");
        return guards.lockMode(rtId);
    }
    private WmRtVendorLine existing(Long id) {
        WmRtVendorLine row=id==null?null:wmRtVendorLineMapper.selectWmRtVendorLineByLineId(id);
        if (row==null || row.getRtId()==null) throw new ServiceException("退供行不存在");
        return row;
    }
    private void assertOwner(WmRtVendorLine row) {
        if (!Objects.equals(row.getRtId(),locks.rtVendorIdForLine(row.getLineId())))
            throw new ServiceException("退供行已变更，请刷新重试");
    }
    private void deletePickingDetails(Long rtId, Long lineId) {
        WmRtVendorDetail query = new WmRtVendorDetail();
        query.setRtId(rtId);
        query.setLineId(lineId);
        List<WmRtVendorDetail> rows = wmRtVendorDetailMapper.selectWmRtVendorDetailList(query);
        if (!rows.isEmpty()) {
            Long[] ids = rows.stream().map(WmRtVendorDetail::getDetailId).toArray(Long[]::new);
            wmRtVendorDetailMapper.deleteWmRtVendorDetailByDetailIds(ids);
        }
    }

    /**
     * 查询供应商退货行
     * 
     * @param lineId 供应商退货行主键
     * @return 供应商退货行
     */
    @Override
    public WmRtVendorLine selectWmRtVendorLineByLineId(Long lineId)
    {
        return wmRtVendorLineMapper.selectWmRtVendorLineByLineId(lineId);
    }

    /**
     * 查询供应商退货行列表
     * 
     * @param wmRtVendorLine 供应商退货行
     * @return 供应商退货行
     */
    @Override
    public List<WmRtVendorLine> selectWmRtVendorLineList(WmRtVendorLine wmRtVendorLine)
    {
        return wmRtVendorLineMapper.selectWmRtVendorLineList(wmRtVendorLine);
    }

    @Override
    public List<WmRtVendorLine> selectWmRtVendorLineListWithDetail(WmRtVendorLine wmRtVendorLine) {
        return wmRtVendorLineMapper.selectWmRtVendorLineListWithDetail(wmRtVendorLine);
    }

    /**
     * 新增供应商退货行
     * 
     * @param wmRtVendorLine 供应商退货行
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWmRtVendorLine(WmRtVendorLine wmRtVendorLine)
    {
        String mode=lockDraft(wmRtVendorLine.getRtId());
        if (wmRtVendorLine.getQuantityRted()==null || wmRtVendorLine.getQuantityRted().signum()<=0)
            throw new ServiceException("退供数量必须大于零");
        if ("PRE_STOCK".equals(mode)) {
            WmRtVendorLine q=new WmRtVendorLine();q.setRtId(wmRtVendorLine.getRtId());
            if (!wmRtVendorLineMapper.selectWmRtVendorLineList(q).isEmpty())
                throw new ServiceException("未入库退供只能保留来源检验行");
        }
        wmRtVendorLine.setCreateTime(DateUtils.getNowDate());
        return wmRtVendorLineMapper.insertWmRtVendorLine(wmRtVendorLine);
    }

    /**
     * 修改供应商退货行
     * 
     * @param wmRtVendorLine 供应商退货行
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWmRtVendorLine(WmRtVendorLine wmRtVendorLine)
    {
        WmRtVendorLine db=existing(wmRtVendorLine.getLineId());String mode=lockDraft(db.getRtId());assertOwner(db);
        if (wmRtVendorLine.getQuantityRted()!=null && wmRtVendorLine.getQuantityRted().signum()<=0)
            throw new ServiceException("退供数量必须大于零");
        if ((wmRtVendorLine.getRtId()!=null && !Objects.equals(db.getRtId(),wmRtVendorLine.getRtId()))
                || (("PRE_STOCK".equals(mode)||"STOCK".equals(mode))
                    && ((wmRtVendorLine.getAttr1()!=null && !Objects.equals(db.getAttr1(),wmRtVendorLine.getAttr1()))
                    || (wmRtVendorLine.getAttr2()!=null && !Objects.equals(db.getAttr2(),wmRtVendorLine.getAttr2()))
                    || (wmRtVendorLine.getItemId()!=null && !Objects.equals(db.getItemId(),wmRtVendorLine.getItemId()))
                    || (wmRtVendorLine.getBatchId()!=null && !Objects.equals(db.getBatchId(),wmRtVendorLine.getBatchId()))
                    || (wmRtVendorLine.getBatchCode()!=null && !Objects.equals(db.getBatchCode(),wmRtVendorLine.getBatchCode())))))
            throw new ServiceException("退供行来源、物料或批次不能修改");
        wmRtVendorLine.setUpdateTime(DateUtils.getNowDate());
        return wmRtVendorLineMapper.updateWmRtVendorLine(wmRtVendorLine);
    }

    /**
     * 批量删除供应商退货行
     * 
     * @param lineIds 需要删除的供应商退货行主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmRtVendorLineByLineIds(Long[] lineIds)
    {
        TreeSet<Long> parents=new TreeSet<Long>();java.util.ArrayList<WmRtVendorLine> rows=new java.util.ArrayList<WmRtVendorLine>();
        for(Long id:lineIds){WmRtVendorLine row=existing(id);rows.add(row);parents.add(row.getRtId());}
        for(Long id:parents) if("PRE_STOCK".equals(lockDraft(id))) throw new ServiceException("未入库退供不能删除来源行");
        for(WmRtVendorLine row:rows) assertOwner(row);
        for(WmRtVendorLine row:rows) deletePickingDetails(row.getRtId(), row.getLineId());
        return wmRtVendorLineMapper.deleteWmRtVendorLineByLineIds(lineIds);
    }

    /**
     * 删除供应商退货行信息
     * 
     * @param lineId 供应商退货行主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmRtVendorLineByLineId(Long lineId)
    {
        WmRtVendorLine row=existing(lineId);
        if("PRE_STOCK".equals(lockDraft(row.getRtId()))) throw new ServiceException("未入库退供不能删除来源行");
        assertOwner(row);
        deletePickingDetails(row.getRtId(), row.getLineId());
        return wmRtVendorLineMapper.deleteWmRtVendorLineByLineId(lineId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByRtId(Long rtId) {
        lockDraft(rtId);
        deletePickingDetails(rtId, null);
        return wmRtVendorLineMapper.deleteByRtId(rtId);
    }
}
