package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.qc.domain.QcOqc;
import com.ktg.mes.qc.domain.QcOqcLine;
import com.ktg.mes.qc.domain.QcTemplateIndex;
import com.ktg.mes.qc.domain.QcTemplateProduct;
import com.ktg.mes.qc.service.IQcOqcLineService;
import com.ktg.mes.qc.service.IQcOqcService;
import com.ktg.mes.qc.service.IQcTemplateIndexService;
import com.ktg.mes.qc.service.IQcTemplateProductService;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import com.ktg.mes.wm.service.IWmProductSalesService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 销售出库提交待检时，按必检出库行幂等生成出货检验草稿。 */
@Service
public class SalesQualityLinkageService {

    private final IQcOqcService oqcService;
    private final IQcTemplateProductService templateProductService;
    private final IQcTemplateIndexService templateIndexService;
    private final IQcOqcLineService oqcLineService;
    private final AutoCodeUtil autoCodeUtil;
    private final MenuLinkageNoticeService menuNoticeService;
    private final IWmProductSalesService productSalesService;

    public SalesQualityLinkageService(IQcOqcService oqcService,
                                      IQcTemplateProductService templateProductService,
                                      IQcTemplateIndexService templateIndexService,
                                      IQcOqcLineService oqcLineService,
                                      AutoCodeUtil autoCodeUtil,
                                      MenuLinkageNoticeService menuNoticeService,
                                      IWmProductSalesService productSalesService) {
        this.oqcService = oqcService;
        this.templateProductService = templateProductService;
        this.templateIndexService = templateIndexService;
        this.oqcLineService = oqcLineService;
        this.autoCodeUtil = autoCodeUtil;
        this.menuNoticeService = menuNoticeService;
        this.productSalesService = productSalesService;
    }

    @Transactional
    public List<QcOqc> createDrafts(WmProductSales sales,
                                    List<WmProductSalesLine> lines,
                                    String operator) {
        if (sales == null || lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        // 来源快照以库为准：PC/移动端提交待检时请求体往往只带 salesId + status，
        // 客户、销售单号等冗余字段为空；用持久化的销售单覆盖，避免 OQC 草稿 client_id 为空导致插入失败。
        if (sales.getSalesId() != null) {
            WmProductSales persisted = productSalesService.selectWmProductSalesBySalesId(sales.getSalesId());
            if (persisted != null) {
                sales = persisted;
            }
        }
        List<QcOqc> result = new ArrayList<>();
        boolean created = false;
        for (WmProductSalesLine source : lines) {
            if (!UserConstants.YES.equals(source.getOqcCheck())) {
                continue;
            }
            QcOqc query = new QcOqc();
            query.setSourceDocType(UserConstants.SOURCE_DOC_TYPE_PRODUCT_SALSE);
            query.setSourceLineId(source.getLineId());
            List<QcOqc> existing = oqcService.selectQcOqcList(query);
            if (existing != null && !existing.isEmpty()) {
                result.add(existing.get(0));
                continue;
            }

            QcTemplateProduct templateQuery = new QcTemplateProduct();
            templateQuery.setItemId(source.getItemId());
            List<QcTemplateProduct> templates =
                    templateProductService.selectQcTemplateProductList(templateQuery);
            if (templates == null || templates.isEmpty()) {
                throw new ServiceException("产品" + source.getItemCode() + "未配置出货检验模板");
            }
            QcOqc draft = buildDraft(sales, source, templates.get(0), operator);
            oqcService.insertQcOqc(draft);
            createInspectionLines(draft);
            result.add(draft);
            created = true;
        }
        if (created) {
            menuNoticeService.markUnread(MenuLinkageNoticePaths.OQC);
        // 待检任务工作台同步点亮，便于统一入口查看所有待检草稿
        menuNoticeService.markUnread(MenuLinkageNoticePaths.PENDING_INSPECT);
        }
        return result;
    }

    private QcOqc buildDraft(WmProductSales sales,
                              WmProductSalesLine source,
                              QcTemplateProduct template,
                              String operator) {
        QcOqc draft = new QcOqc();
        draft.setOqcCode(autoCodeUtil.genSerialCode(UserConstants.QC_OQC_CODE, null));
        draft.setOqcName("销售出库" + sales.getSalesCode() + "-" + source.getItemName());
        draft.setTemplateId(template.getTemplateId());
        draft.setSourceDocId(sales.getSalesId());
        draft.setSourceDocType(UserConstants.SOURCE_DOC_TYPE_PRODUCT_SALSE);
        draft.setSourceDocCode(sales.getSalesCode());
        draft.setSourceLineId(source.getLineId());
        draft.setClientId(sales.getClientId());
        draft.setClientCode(sales.getClientCode());
        draft.setClientName(sales.getClientName());
        draft.setBatchCode(source.getBatchCode());
        draft.setItemId(source.getItemId());
        draft.setItemCode(source.getItemCode());
        draft.setItemName(source.getItemName());
        draft.setSpecification(source.getSpecification());
        draft.setUnitOfMeasure(source.getUnitOfMeasure());
        draft.setQuantityOut(source.getQuantitySales());
        draft.setQuantityCheck(source.getQuantitySales());
        draft.setQuantityQualified(BigDecimal.ZERO);
        draft.setQuantityUnqualified(BigDecimal.ZERO);
        if (template.getQuantityCheck() != null) {
            draft.setQuantityMinCheck(BigDecimal.valueOf(template.getQuantityCheck()));
        }
        if (template.getQuantityUnqualified() != null) {
            draft.setQuantityMaxUnqualified(BigDecimal.valueOf(template.getQuantityUnqualified()));
        }
        draft.setOutDate(sales.getSalesDate());
        draft.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        draft.setAttr1("AUTO:OQC:PRODUCT_SALES:" + source.getLineId());
        draft.setCreateBy(operator);
        return draft;
    }

    private void createInspectionLines(QcOqc draft) {
        QcTemplateIndex query = new QcTemplateIndex();
        query.setTemplateId(draft.getTemplateId());
        List<QcTemplateIndex> indexes = templateIndexService.selectQcTemplateIndexList(query);
        if (indexes == null) {
            return;
        }
        for (QcTemplateIndex index : indexes) {
            QcOqcLine line = new QcOqcLine();
            line.setOqcId(draft.getOqcId());
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
            oqcLineService.insertQcOqcLine(line);
        }
    }
}
