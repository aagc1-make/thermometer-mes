package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.qc.domain.QcIqc;
import com.ktg.mes.qc.domain.QcIqcLine;
import com.ktg.mes.qc.domain.QcTemplateIndex;
import com.ktg.mes.qc.domain.QcTemplateProduct;
import com.ktg.mes.qc.service.IQcIqcLineService;
import com.ktg.mes.qc.service.IQcIqcService;
import com.ktg.mes.qc.service.IQcTemplateIndexService;
import com.ktg.mes.qc.service.IQcTemplateProductService;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 到货通知单提交后，按需检行幂等生成来料检验草稿。 */
@Service
public class ArrivalQualityLinkageService {

    private final IQcIqcService iqcService;
    private final IQcTemplateProductService templateProductService;
    private final IQcTemplateIndexService templateIndexService;
    private final IQcIqcLineService iqcLineService;
    private final AutoCodeUtil autoCodeUtil;
    private final MenuLinkageNoticeService menuNoticeService;

    public ArrivalQualityLinkageService(IQcIqcService iqcService,
                                        IQcTemplateProductService templateProductService,
                                        IQcTemplateIndexService templateIndexService,
                                        IQcIqcLineService iqcLineService,
                                        AutoCodeUtil autoCodeUtil,
                                        MenuLinkageNoticeService menuNoticeService) {
        this.iqcService = iqcService;
        this.templateProductService = templateProductService;
        this.templateIndexService = templateIndexService;
        this.iqcLineService = iqcLineService;
        this.autoCodeUtil = autoCodeUtil;
        this.menuNoticeService = menuNoticeService;
    }

    @Transactional
    public List<QcIqc> createDrafts(WmArrivalNotice notice,
                                    List<WmArrivalNoticeLine> lines,
                                    String operator) {
        if (notice == null || lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        List<QcIqc> result = new ArrayList<>();
        boolean created = false;
        for (WmArrivalNoticeLine line : lines) {
            if (!UserConstants.YES.equals(line.getIqcCheck())) {
                continue;
            }
            QcIqc query = new QcIqc();
            query.setSourceDocType(UserConstants.SOURCE_DOC_TYPE_ARRIVALNOTICE);
            query.setSourceLineId(line.getLineId());
            List<QcIqc> existing = iqcService.selectQcIqcList(query);
            if (existing != null && !existing.isEmpty()) {
                result.add(existing.get(0));
                continue;
            }

            QcTemplateProduct templateQuery = new QcTemplateProduct();
            templateQuery.setItemId(line.getItemId());
            List<QcTemplateProduct> templates =
                    templateProductService.selectQcTemplateProductList(templateQuery);
            if (templates == null || templates.isEmpty()) {
                throw new ServiceException("物料" + line.getItemCode() + "未配置来料检验模板");
            }

            QcTemplateProduct template = templates.get(0);
            QcIqc draft = buildDraft(notice, line, template, operator);
            iqcService.insertQcIqc(draft);
            createInspectionLines(draft);
            result.add(draft);
            created = true;
        }
        if (created) {
            menuNoticeService.markUnread(MenuLinkageNoticePaths.IQC);
        }
        return result;
    }

    private QcIqc buildDraft(WmArrivalNotice notice,
                              WmArrivalNoticeLine line,
                              QcTemplateProduct template,
                              String operator) {
        QcIqc draft = new QcIqc();
        draft.setIqcCode(autoCodeUtil.genSerialCode(UserConstants.QC_IQC_CODE, null));
        draft.setIqcName("到货通知" + notice.getNoticeCode() + "-" + line.getItemName());
        draft.setTemplateId(template.getTemplateId());
        draft.setSourceDocId(notice.getNoticeId());
        draft.setSourceDocType(UserConstants.SOURCE_DOC_TYPE_ARRIVALNOTICE);
        draft.setSourceDocCode(notice.getNoticeCode());
        draft.setSourceLineId(line.getLineId());
        draft.setVendorId(notice.getVendorId());
        draft.setVendorCode(notice.getVendorCode());
        draft.setVendorName(notice.getVendorName());
        draft.setVendorNick(notice.getVendorNick());
        draft.setItemId(line.getItemId());
        draft.setItemCode(line.getItemCode());
        draft.setItemName(line.getItemName());
        draft.setSpecification(line.getSpecification());
        draft.setUnitOfMeasure(line.getUnitOfMeasure());
        draft.setUnitName(line.getUnitName());
        draft.setQuantityRecived(line.getQuantityArrival());
        draft.setQuantityCheck(line.getQuantityArrival());
        draft.setQuantityQualified(BigDecimal.ZERO);
        draft.setQuantityUnqualified(BigDecimal.ZERO);
        if (template.getQuantityCheck() != null) {
            draft.setQuantityMinCheck(BigDecimal.valueOf(template.getQuantityCheck()));
        }
        if (template.getQuantityUnqualified() != null) {
            draft.setQuantityMaxUnqualified(BigDecimal.valueOf(template.getQuantityUnqualified()));
        }
        draft.setReciveDate(notice.getArrivalDate());
        draft.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        draft.setAttr1("AUTO:IQC:ARRIVAL_NOTICE:" + line.getLineId());
        draft.setCreateBy(operator);
        return draft;
    }

    private void createInspectionLines(QcIqc draft) {
        QcTemplateIndex query = new QcTemplateIndex();
        query.setTemplateId(draft.getTemplateId());
        List<QcTemplateIndex> indexes = templateIndexService.selectQcTemplateIndexList(query);
        if (indexes == null) {
            return;
        }
        for (QcTemplateIndex index : indexes) {
            QcIqcLine line = new QcIqcLine();
            line.setIqcId(draft.getIqcId());
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
            iqcLineService.insertQcIqcLine(line);
        }
    }
}
