package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import com.ktg.mes.wm.domain.WmSalesNotice;
import com.ktg.mes.wm.domain.WmSalesNoticeLine;
import com.ktg.mes.wm.mapper.ProductReceiptGuardMapper;
import com.ktg.mes.wm.service.IWmProductSalesLineService;
import com.ktg.mes.wm.service.IWmProductSalesService;
import com.ktg.mes.wm.service.IWmSalesNoticeService;
import com.ktg.system.service.CodeSequenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 发货通知单确认后，幂等生成销售出库草稿。 */
@Service
public class SalesNoticeLinkageService {

    private final IWmProductSalesService salesService;
    private final IWmProductSalesLineService salesLineService;
    private final CodeSequenceService codeSequenceService;
    private final MenuLinkageNoticeService menuNoticeService;
    private final IWmSalesNoticeService salesNoticeService;
    private final ProductReceiptGuardMapper receiptGuardMapper;

    public SalesNoticeLinkageService(IWmProductSalesService salesService,
                                     IWmProductSalesLineService salesLineService,
                                     CodeSequenceService codeSequenceService,
                                     MenuLinkageNoticeService menuNoticeService,
                                     IWmSalesNoticeService salesNoticeService,
                                     ProductReceiptGuardMapper receiptGuardMapper) {
        this.salesService = salesService;
        this.salesLineService = salesLineService;
        this.codeSequenceService = codeSequenceService;
        this.menuNoticeService = menuNoticeService;
        this.salesNoticeService = salesNoticeService;
        this.receiptGuardMapper = receiptGuardMapper;
    }

    @Transactional
    public WmProductSales createDraft(WmSalesNotice notice,
                                      List<WmSalesNoticeLine> sourceLines,
                                      String operator) {
        // 来源快照以库为准：确认接口的请求体通常只带 noticeId + status，
        // 客户/收货信息等冗余字段为空；用持久化的发货通知覆盖，避免销售草稿 client_code 为空。
        if (notice.getNoticeId() != null) {
            WmSalesNotice persisted = salesNoticeService.selectWmSalesNoticeByNoticeId(notice.getNoticeId());
            if (persisted != null) {
                notice = persisted;
            }
        }
        WmProductSales query = new WmProductSales();
        query.setNoticeId(notice.getNoticeId());
        List<WmProductSales> existing = salesService.selectWmProductSalesList(query);
        if (existing != null && !existing.isEmpty()) {
            // 草稿在订单确认时已预建；发货通知补填批次后，把批次同步到出库草稿行
            syncLineBatches(existing.get(0), sourceLines);
            return existing.get(0);
        }

        WmProductSales sales = new WmProductSales();
        sales.setSalesCode(codeSequenceService.generateChainCode(UserConstants.CODE_RULE_PRODUCT_SALES, notice.getSoCode()));
        sales.setSalesName("发货通知" + notice.getNoticeCode() + "销售出库");
        sales.setNoticeId(notice.getNoticeId());
        sales.setNoticeCode(notice.getNoticeCode());
        sales.setSoCode(notice.getSoCode());
        sales.setClientId(notice.getClientId());
        sales.setClientCode(notice.getClientCode());
        sales.setClientName(notice.getClientName());
        sales.setClientNick(notice.getClientNick());
        sales.setRecipient(notice.getRecipient());
        sales.setTel(notice.getTel());
        sales.setAddress(notice.getAddress());
        sales.setSalesDate(notice.getSalesDate());
        sales.setStatus(UserConstants.PRODUCT_SALES_STATUS_PREPARE);
        sales.setAttr1("AUTO:PRODUCT_SALES:SALES_NOTICE:" + notice.getNoticeId());
        sales.setCreateBy(operator);
        salesService.insertWmProductSales(sales);

        for (WmSalesNoticeLine source : sourceLines) {
            WmProductSalesLine line = new WmProductSalesLine();
            line.setSalesId(sales.getSalesId());
            line.setItemId(source.getItemId());
            line.setItemCode(source.getItemCode());
            line.setItemName(source.getItemName());
            line.setSpecification(source.getSpecification());
            line.setUnitOfMeasure(source.getUnitOfMeasure());
            line.setUnitName(source.getUnitName());
            line.setQuantitySales(source.getQuantitySales());
            line.setBatchId(source.getBatchId());
            line.setBatchCode(source.getBatchCode());
            line.setOqcCheck(source.getOqcCheck());
            // 出库行必须与工单、成品批次严格对应：按订单已入库记录反查并绑定
            bindWorkorderAndBatch(line, notice.getSoCode(), source.getBatchCode());
            line.setAttr1("AUTO:SALES_LINE:NOTICE:" + source.getLineId());
            line.setCreateBy(operator);
            salesLineService.insertWmProductSalesLine(line);
        }
        menuNoticeService.markUnread(MenuLinkageNoticePaths.PRODUCT_SALES);
        return sales;
    }

    /** 把发货通知行上的批次同步到尚未执行的销售出库草稿行，保证出库单批次自动绑定。 */
    private void syncLineBatches(WmProductSales draft, List<WmSalesNoticeLine> sourceLines) {
        if (draft == null || sourceLines == null || sourceLines.isEmpty()
                || !UserConstants.PRODUCT_SALES_STATUS_PREPARE.equals(draft.getStatus())) {
            return;
        }
        WmProductSalesLine query = new WmProductSalesLine();
        query.setSalesId(draft.getSalesId());
        List<WmProductSalesLine> draftLines = salesLineService.selectWmProductSalesLineList(query);
        if (draftLines == null || draftLines.isEmpty()) {
            return;
        }
        for (WmSalesNoticeLine source : sourceLines) {
            if (source.getBatchId() == null && source.getBatchCode() == null) {
                continue;
            }
            for (WmProductSalesLine line : draftLines) {
                boolean sameItem = (source.getItemId() != null && source.getItemId().equals(line.getItemId()))
                        || (source.getItemCode() != null && source.getItemCode().equals(line.getItemCode()));
                if (!sameItem || line.getBatchId() != null || line.getBatchCode() != null) {
                    continue;
                }
                line.setBatchId(source.getBatchId());
                line.setBatchCode(source.getBatchCode());
                bindWorkorderAndBatch(line, draft.getSoCode(), source.getBatchCode());
                salesLineService.updateWmProductSalesLine(line);
            }
        }
    }

    /**
     * 销售出库行补齐工单与成品批次绑定。
     * 批次优先（同订单可能有多张工单），没有批次时按订单+成品取已入库记录。
     */
    private void bindWorkorderAndBatch(WmProductSalesLine line, String soCode, String batchCode) {
        if (line == null || soCode == null || soCode.isEmpty() || line.getItemId() == null) {
            return;
        }
        if (line.getWorkorderId() != null && line.getBatchCode() != null) {
            return;
        }
        Map<String, Object> binding;
        try {
            binding = receiptGuardMapper.finishedRecptBinding(soCode, line.getItemId(),
                    line.getBatchCode() != null ? line.getBatchCode() : batchCode);
        } catch (Exception ignored) {
            // 取不到绑定不影响出库单据生成，流水线会按订单号兜底统计
            return;
        }
        if (binding == null) {
            return;
        }
        if (line.getWorkorderId() == null && binding.get("workorderId") != null) {
            line.setWorkorderId(Long.valueOf(String.valueOf(binding.get("workorderId"))));
            line.setWorkorderCode(binding.get("workorderCode") == null ? null : String.valueOf(binding.get("workorderCode")));
        }
        if (line.getBatchCode() == null && binding.get("batchCode") != null) {
            line.setBatchCode(String.valueOf(binding.get("batchCode")));
            if (binding.get("batchId") != null) {
                line.setBatchId(Long.valueOf(String.valueOf(binding.get("batchId"))));
            }
        }
    }
}
