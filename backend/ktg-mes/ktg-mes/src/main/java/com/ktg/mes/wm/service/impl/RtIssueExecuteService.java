package com.ktg.mes.wm.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.domain.WmRtIssue;
import com.ktg.mes.wm.domain.WmRtIssueDetail;
import com.ktg.mes.wm.domain.WmRtIssueLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.service.IStorageCoreService;
import com.ktg.mes.wm.service.IWmRtIssueDetailService;
import com.ktg.mes.wm.service.IWmRtIssueLineService;
import com.ktg.mes.wm.service.IWmRtIssueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** 生产退料过账的共同入口：状态、质量、明细和库存处于同一事务。 */
@Service
public class RtIssueExecuteService {
    private final PostingLockMapper locks;
    private final IWmRtIssueService headers;
    private final IWmRtIssueLineService lines;
    private final IWmRtIssueDetailService details;
    private final IStorageCoreService storage;

    public RtIssueExecuteService(PostingLockMapper locks, IWmRtIssueService headers,
                                 IWmRtIssueLineService lines, IWmRtIssueDetailService details,
                                 IStorageCoreService storage) {
        this.locks=locks; this.headers=headers; this.lines=lines; this.details=details; this.storage=storage;
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(Long id) {
        if (id == null || !UserConstants.RTISSUE_STATUS_UNEXECUTE.equals(locks.rtIssue(id)))
            throw new ServiceException("退料单不存在、已执行或不在待执行状态");
        locks.inventory();
        WmRtIssueLine filter = new WmRtIssueLine(); filter.setRtId(id);
        List<WmRtIssueLine> records = lines.selectWmRtIssueLineList(filter);
        if (records == null || records.isEmpty()) throw new ServiceException("请添加退料物资");
        for (WmRtIssueLine line : records) {
            if (!UserConstants.QUALITY_STATUS_PASS.equals(line.getQualityStatus()))
                throw new ServiceException("物料【" + line.getItemCode() + "】尚未检验合格，不能退回库存");
            if (line.getQuantityRt() == null || line.getQuantityRt().signum() <= 0)
                throw new ServiceException("退料数量必须大于零");
            WmRtIssueDetail query = new WmRtIssueDetail(); query.setRtId(id); query.setLineId(line.getLineId());
            List<WmRtIssueDetail> allocations = details.selectWmRtIssueDetailList(query);
            if (allocations == null || allocations.isEmpty()) throw new ServiceException("退料行尚未上架");
            BigDecimal total = BigDecimal.ZERO;
            for (WmRtIssueDetail detail : allocations) {
                if (!Objects.equals(id, detail.getRtId()) || !Objects.equals(line.getLineId(), detail.getLineId())
                        || !Objects.equals(line.getItemId(), detail.getItemId())
                        || !Objects.equals(line.getBatchId(), detail.getBatchId())
                        || !Objects.equals(line.getBatchCode(), detail.getBatchCode()))
                    throw new ServiceException("退料明细与来源行的物料或批次不一致");
                if (detail.getWarehouseId() == null || detail.getLocationId() == null || detail.getAreaId() == null)
                    throw new ServiceException("退料明细缺少目标仓库、库区或库位");
                if (detail.getQuantity() == null || detail.getQuantity().signum() <= 0)
                    throw new ServiceException("退料上架数量必须大于零");
                total = total.add(detail.getQuantity());
            }
            if (total.compareTo(line.getQuantityRt()) != 0)
                throw new ServiceException("退料上架明细合计必须等于退料数量");
        }
        WmRtIssue header = headers.selectWmRtIssueByRtId(id);
        if (header == null) throw new ServiceException("退料单不存在");
        storage.processRtIssue(headers.getTxBeans(id));
        header.setStatus(UserConstants.RTISSUE_STATUS_FINISHED);
        if (headers.updateWmRtIssue(header) != 1) throw new ServiceException("退料单状态更新失败");
    }
}
