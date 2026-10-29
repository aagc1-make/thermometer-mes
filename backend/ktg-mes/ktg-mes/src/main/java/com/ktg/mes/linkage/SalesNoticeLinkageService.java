package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import com.ktg.mes.wm.domain.WmSalesNotice;
import com.ktg.mes.wm.domain.WmSalesNoticeLine;
import com.ktg.mes.wm.service.IWmProductSalesLineService;
import com.ktg.mes.wm.service.IWmProductSalesService;
import com.ktg.mes.wm.service.IWmSalesNoticeService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 发货通知单确认后，幂等生成销售出库草稿。 */
@Service
public class SalesNoticeLinkageService {

    private final IWmProductSalesService salesService;
    private final IWmProductSalesLineService salesLineService;
    private final AutoCodeUtil autoCodeUtil;
    private final MenuLinkageNoticeService menuNoticeService;
    private final IWmSalesNoticeService salesNoticeService;

    public SalesNoticeLinkageService(IWmProductSalesService salesService,
                                     IWmProductSalesLineService salesLineService,
                                     AutoCodeUtil autoCodeUtil,
                                     MenuLinkageNoticeService menuNoticeService,
                                     IWmSalesNoticeService salesNoticeService) {
        this.salesService = salesService;
        this.salesLineService = salesLineService;
        this.autoCodeUtil = autoCodeUtil;
        this.menuNoticeService = menuNoticeService;
        this.salesNoticeService = salesNoticeService;
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
            return existing.get(0);
        }

        WmProductSales sales = new WmProductSales();
        sales.setSalesCode(autoCodeUtil.genSerialCode(UserConstants.PRODUCTSALSE_CODE, ""));
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
            line.setAttr1("AUTO:SALES_LINE:NOTICE:" + source.getLineId());
            line.setCreateBy(operator);
            salesLineService.insertWmProductSalesLine(line);
        }
        menuNoticeService.markUnread(MenuLinkageNoticePaths.PRODUCT_SALES);
        return sales;
    }
}
