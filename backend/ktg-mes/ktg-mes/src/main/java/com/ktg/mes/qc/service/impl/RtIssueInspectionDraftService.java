package com.ktg.mes.qc.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.qc.domain.QcRqc;
import com.ktg.mes.qc.domain.QcTemplateProduct;
import com.ktg.mes.qc.service.IQcRqcService;
import com.ktg.mes.qc.service.IQcTemplateProductService;
import com.ktg.mes.wm.domain.WmRtIssue;
import com.ktg.mes.wm.domain.WmRtIssueLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.service.IWmRtIssueLineService;
import com.ktg.mes.wm.service.IWmRtIssueService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 生产退料提交待检时生成 RQC 草稿（PC 与移动端共用）。
 *
 * 规则：
 * - 只对“需检(qcFlag=Y)且未检(qualityStatus=NT)”的退料行生成；
 * - 同一退料行已有未取消的检验单时复用，不重复建单（并发由退料单行锁串行化）；
 * - 检测数量默认取退料数量，合格/不合格留空由检验员填写，避免把未检数据写成合格。
 */
@Service
public class RtIssueInspectionDraftService {
    private final PostingLockMapper locks;
    private final IWmRtIssueService issues;
    private final IWmRtIssueLineService lines;
    private final IQcRqcService rqcs;
    private final IQcTemplateProductService templates;
    private final AutoCodeUtil codes;
    private final QcRqcItemLineService itemLines;

    public RtIssueInspectionDraftService(PostingLockMapper locks, IWmRtIssueService issues,
                                         IWmRtIssueLineService lines, IQcRqcService rqcs,
                                         IQcTemplateProductService templates, AutoCodeUtil codes,
                                         QcRqcItemLineService itemLines) {
        this.locks = locks;
        this.issues = issues;
        this.lines = lines;
        this.rqcs = rqcs;
        this.templates = templates;
        this.codes = codes;
        this.itemLines = itemLines;
    }

    /** 为待检退料单补齐 RQC 草稿，返回本次新建数量。调用方须处于事务内。 */
    @Transactional(rollbackFor = Exception.class)
    public int ensureDrafts(Long rtId, String operator) {
        if (rtId == null) {
            throw new ServiceException("缺少退料单");
        }
        String status = locks.rtIssue(rtId);
        if (!UserConstants.RTISSUE_STATUS_UNCHECK.equals(status)) {
            return 0;
        }
        WmRtIssue issue = issues.selectWmRtIssueByRtId(rtId);
        if (issue == null) {
            throw new ServiceException("退料单不存在");
        }
        WmRtIssueLine filter = new WmRtIssueLine();
        filter.setRtId(rtId);
        List<WmRtIssueLine> rows = lines.selectWmRtIssueLineList(filter);
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        int created = 0;
        for (WmRtIssueLine row : rows) {
            if (!UserConstants.YES.equals(row.getQcFlag())
                    || !UserConstants.QUALITY_STATUS_NOTTEST.equals(row.getQualityStatus())) {
                continue;
            }
            if (locks.rqcIdForRtIssueLine(row.getLineId()) != null) {
                continue;
            }
            QcRqc rqc = new QcRqc();
            rqc.setRqcCode(codes.genSerialCode(UserConstants.QC_RQC_CODE, null));
            rqc.setRqcName("生产退料检验-" + (issue.getRtCode() == null ? rtId : issue.getRtCode()));
            rqc.setSourceDocId(rtId);
            rqc.setSourceDocType(UserConstants.SOURCE_DOC_TYPE_RTISSUE);
            rqc.setSourceDocCode(issue.getRtCode());
            rqc.setSourceLineId(row.getLineId());
            rqc.setItemId(row.getItemId());
            rqc.setItemCode(row.getItemCode());
            rqc.setItemName(row.getItemName());
            rqc.setSpecification(row.getSpecification());
            rqc.setUnitOfMeasure(row.getUnitOfMeasure());
            rqc.setUnitName(row.getUnitName());
            rqc.setBatchId(row.getBatchId());
            rqc.setBatchCode(row.getBatchCode());
            rqc.setQuantityCheck(row.getQuantityRt());
            rqc.setStatus(UserConstants.ORDER_STATUS_PREPARE);
            QcTemplateProduct param = new QcTemplateProduct();
            param.setItemId(row.getItemId());
            List<QcTemplateProduct> found = templates.selectQcTemplateProductList(param);
            if (found == null || found.isEmpty() || found.get(0).getTemplateId() == null) {
                throw new ServiceException("物料【" + row.getItemCode() + "】未配置检测模板，无法生成退料检验单；请先在质检方案中配置");
            }
            rqc.setTemplateId(found.get(0).getTemplateId());
            rqc.setCreateBy(operator);
            if (rqcs.insertQcRqc(rqc) != 1) {
                throw new ServiceException("生成退料检验草稿失败");
            }
            // 与手工新增 RQC 共用同一套检测项目生成逻辑，保证页面能看到模板检测项
            itemLines.generate(rqc);
            created++;
        }
        return created;
    }
}
