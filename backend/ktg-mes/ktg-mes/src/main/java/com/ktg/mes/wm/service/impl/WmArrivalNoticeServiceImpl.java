package com.ktg.mes.wm.service.impl;

import java.util.List;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.utils.DateUtils;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.mapper.WmArrivalNoticeLineMapper;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ktg.mes.wm.mapper.WmArrivalNoticeMapper;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.service.IWmArrivalNoticeService;
import org.springframework.util.CollectionUtils;

/**
 * 到货通知单Service业务层处理
 * 
 * @author yinjinlu
 * @date 2024-11-12
 */
@Service
public class WmArrivalNoticeServiceImpl implements IWmArrivalNoticeService 
{
    @Autowired
    private WmArrivalNoticeMapper wmArrivalNoticeMapper;

    @Autowired
    private WmArrivalNoticeLineMapper wmArrivalNoticeLineMapper;

    /**
     * 查询到货通知单
     * 
     * @param noticeId 到货通知单主键
     * @return 到货通知单
     */
    @Override
    public WmArrivalNotice selectWmArrivalNoticeByNoticeId(Long noticeId)
    {
        return wmArrivalNoticeMapper.selectWmArrivalNoticeByNoticeId(noticeId);
    }

    /**
     * 查询到货通知单列表
     * 
     * @param wmArrivalNotice 到货通知单
     * @return 到货通知单
     */
    @Override
    public List<WmArrivalNotice> selectWmArrivalNoticeList(WmArrivalNotice wmArrivalNotice)
    {
        return wmArrivalNoticeMapper.selectWmArrivalNoticeList(wmArrivalNotice);
    }

    /**
     * 检查通知单编码是否唯一
     * @param wmArrivalNotice
     * @return
     */
    @Override
    public String checkRnCodeUnique(WmArrivalNotice wmArrivalNotice) {
        WmArrivalNotice notice = wmArrivalNoticeMapper.checkRnCodeUnique(wmArrivalNotice);
        if (StringUtils.isNotNull(notice)) {
            // 联动优化（审阅 P2-10）：原实现用查询结果自身的 ID 与自身比较，永远相等，
            // 无法识别“另一张单占用了同一单号”。应把库里那条单的 ID 与请求对象的 ID 比较。
            Long requestId = wmArrivalNotice.getNoticeId();
            if (requestId == null || !notice.getNoticeId().equals(requestId)) {
                return UserConstants.NOT_UNIQUE;
            }
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 新增到货通知单
     * 
     * @param wmArrivalNotice 到货通知单
     * @return 结果
     */
    @Override
    public int insertWmArrivalNotice(WmArrivalNotice wmArrivalNotice)
    {
        wmArrivalNotice.setCreateTime(DateUtils.getNowDate());
        return wmArrivalNoticeMapper.insertWmArrivalNotice(wmArrivalNotice);
    }

    /**
     * 修改到货通知单
     * 
     * @param wmArrivalNotice 到货通知单
     * @return 结果
     */
    @Override
    public int updateWmArrivalNotice(WmArrivalNotice wmArrivalNotice)
    {
        wmArrivalNotice.setUpdateTime(DateUtils.getNowDate());
        return wmArrivalNoticeMapper.updateWmArrivalNotice(wmArrivalNotice);
    }

    /**
     * 检查通知单下是否还有未检测的行，没有则更新单据状态为待接收APPROVED
     * @param noticeId
     */
    @Override
    public void updateStatus(Long noticeId) {

        List<WmArrivalNoticeLine> lines = wmArrivalNoticeLineMapper.selectUncheckedLine(noticeId);
        if(CollectionUtils.isEmpty(lines)){
            WmArrivalNotice notice = wmArrivalNoticeMapper.selectWmArrivalNoticeByNoticeId(noticeId);
            notice.setStatus(UserConstants.ORDER_STATUS_APPROVED);
            wmArrivalNoticeMapper.updateWmArrivalNotice(notice);
        }
    }

    /**
     * 批量删除到货通知单
     * 
     * @param noticeIds 需要删除的到货通知单主键
     * @return 结果
     */
    @Override
    public int deleteWmArrivalNoticeByNoticeIds(Long[] noticeIds)
    {
        return wmArrivalNoticeMapper.deleteWmArrivalNoticeByNoticeIds(noticeIds);
    }

    /**
     * 删除到货通知单信息
     * 
     * @param noticeId 到货通知单主键
     * @return 结果
     */
    @Override
    public int deleteWmArrivalNoticeByNoticeId(Long noticeId)
    {
        return wmArrivalNoticeMapper.deleteWmArrivalNoticeByNoticeId(noticeId);
    }
}
