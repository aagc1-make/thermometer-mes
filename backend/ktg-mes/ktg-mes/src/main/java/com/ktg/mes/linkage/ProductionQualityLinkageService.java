package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.pro.domain.ProFeedback;
import com.ktg.mes.qc.domain.QcIpqc;
import com.ktg.mes.qc.domain.QcIpqcLine;
import com.ktg.mes.qc.domain.QcTemplate;
import com.ktg.mes.qc.domain.QcTemplateIndex;
import com.ktg.mes.qc.service.IQcIpqcLineService;
import com.ktg.mes.qc.service.IQcIpqcService;
import com.ktg.mes.qc.service.IQcTemplateIndexService;
import com.ktg.mes.qc.service.IQcTemplateService;
import com.ktg.mes.wm.domain.WmProductProduce;
import com.ktg.mes.wm.domain.WmProductProduceLine;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** 关键工序报工产出后，幂等生成过程检验草稿。 */
@Service
public class ProductionQualityLinkageService {

    private static final String IPQC_TYPE = "IPQC";

    private final IQcIpqcService ipqcService;
    private final IQcTemplateService templateService;
    private final IQcTemplateIndexService templateIndexService;
    private final IQcIpqcLineService ipqcLineService;
    private final AutoCodeUtil autoCodeUtil;
    private final MenuLinkageNoticeService menuNoticeService;

    public ProductionQualityLinkageService(IQcIpqcService ipqcService,
                                           IQcTemplateService templateService,
                                           IQcTemplateIndexService templateIndexService,
                                           IQcIpqcLineService ipqcLineService,
                                           AutoCodeUtil autoCodeUtil,
                                           MenuLinkageNoticeService menuNoticeService) {
        this.ipqcService = ipqcService;
        this.templateService = templateService;
        this.templateIndexService = templateIndexService;
        this.ipqcLineService = ipqcLineService;
        this.autoCodeUtil = autoCodeUtil;
        this.menuNoticeService = menuNoticeService;
    }

    @Transactional
    public QcIpqc createDraft(ProFeedback feedback,
                              WmProductProduce produce,
                              WmProductProduceLine source,
                              String operator) {
        QcIpqc query = new QcIpqc();
        query.setSourceDocType(UserConstants.SOURCE_DOC_TYPE_FEEDBACK);
        query.setSourceLineId(source.getLineId());
        List<QcIpqc> existing = ipqcService.selectQcIpqcList(query);
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }

        QcTemplate templateQuery = new QcTemplate();
        templateQuery.setQcTypes(IPQC_TYPE);
        templateQuery.setItemId(source.getItemId());
        QcTemplate template = templateService.selectQcTemplateByProductAndQcType(templateQuery);
        if (template == null) {
            throw new ServiceException("产品" + source.getItemCode() + "未配置过程检验模板");
        }

        QcIpqc draft = new QcIpqc();
        draft.setIpqcCode(autoCodeUtil.genSerialCode(UserConstants.QC_PQC_CODE, null));
        draft.setIpqcName("生产报工" + feedback.getFeedbackCode() + "-" + source.getItemName());
        draft.setIpqcType(IPQC_TYPE);
        draft.setTemplateId(template.getTemplateId());
        draft.setSourceDocId(produce.getRecordId());
        draft.setSourceDocType(UserConstants.SOURCE_DOC_TYPE_FEEDBACK);
        draft.setSourceDocCode(feedback.getFeedbackCode());
        draft.setSourceLineId(source.getLineId());
        draft.setWorkorderId(feedback.getWorkorderId());
        draft.setWorkorderCode(feedback.getWorkorderCode());
        draft.setWorkorderName(feedback.getWorkorderName());
        draft.setTaskId(feedback.getTaskId());
        draft.setTaskCode(feedback.getTaskCode());
        draft.setWorkstationId(feedback.getWorkstationId());
        draft.setWorkstationCode(feedback.getWorkstationCode());
        draft.setWorkstationName(feedback.getWorkstationName());
        draft.setProcessId(feedback.getProcessId());
        draft.setProcessCode(feedback.getProcessCode());
        draft.setProcessName(feedback.getProcessName());
        draft.setItemId(source.getItemId());
        draft.setItemCode(source.getItemCode());
        draft.setItemName(source.getItemName());
        draft.setSpecification(source.getSpecification());
        draft.setUnitOfMeasure(source.getUnitOfMeasure());
        draft.setUnitName(source.getUnitName());
        draft.setQuantityCheck(source.getQuantityProduce());
        draft.setQuantityQualified(BigDecimal.ZERO);
        draft.setQuantityUnqualified(BigDecimal.ZERO);
        draft.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        draft.setAttr1("AUTO:IPQC:FEEDBACK:" + source.getLineId());
        draft.setCreateBy(operator);
        ipqcService.insertQcIpqc(draft);
        createInspectionLines(draft);
        menuNoticeService.markUnread(MenuLinkageNoticePaths.IPQC);
        // 待检任务工作台同步点亮，便于统一入口查看所有待检草稿
        menuNoticeService.markUnread(MenuLinkageNoticePaths.PENDING_INSPECT);
        return draft;
    }

    private void createInspectionLines(QcIpqc draft) {
        QcTemplateIndex query = new QcTemplateIndex();
        query.setTemplateId(draft.getTemplateId());
        List<QcTemplateIndex> indexes = templateIndexService.selectQcTemplateIndexList(query);
        if (indexes == null) {
            return;
        }
        for (QcTemplateIndex index : indexes) {
            QcIpqcLine line = new QcIpqcLine();
            line.setIpqcId(draft.getIpqcId());
            line.setIndexId(index.getIndexId());
            line.setIndexCode(index.getIndexCode());
            line.setIndexName(index.getIndexName());
            line.setIndexType(index.getIndexType());
            line.setQcTool(index.getQcTool());
            line.setCheckMethod(index.getCheckMethod());
            line.setStanderVal(index.getStanderVal());
            line.setUnitOfMeasure(index.getUnitOfMeasure());
            line.setThresholdMax(index.getThresholdMax());
            line.setThresholdMin(index.getThresholdMin());
            line.setCrQuantity(BigDecimal.ZERO);
            line.setMajQuantity(BigDecimal.ZERO);
            line.setMinQuantity(BigDecimal.ZERO);
            ipqcLineService.insertQcIpqcLine(line);
        }
    }
}
