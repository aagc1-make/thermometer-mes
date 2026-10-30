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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalesNoticeLinkageServiceTest {

    private IWmProductSalesService salesService;
    private IWmProductSalesLineService salesLineService;
    private AutoCodeUtil autoCodeUtil;
    private MenuLinkageNoticeService menuNoticeService;
    private IWmSalesNoticeService salesNoticeService;
    private SalesNoticeLinkageService linkageService;

    @BeforeEach
    void setUp() {
        salesService = mock(IWmProductSalesService.class);
        salesLineService = mock(IWmProductSalesLineService.class);
        autoCodeUtil = mock(AutoCodeUtil.class);
        menuNoticeService = mock(MenuLinkageNoticeService.class);
        salesNoticeService = mock(IWmSalesNoticeService.class);
        linkageService = new SalesNoticeLinkageService(
                salesService, salesLineService, autoCodeUtil, menuNoticeService, salesNoticeService);
    }

    @Test
    void confirmingNoticeCreatesSalesDraftAndLines() {
        when(salesService.selectWmProductSalesList(any(WmProductSales.class)))
                .thenReturn(Collections.emptyList());
        when(autoCodeUtil.genSerialCode(UserConstants.PRODUCTSALSE_CODE, ""))
                .thenReturn("SALE-001");
        when(salesService.insertWmProductSales(any(WmProductSales.class))).thenAnswer(invocation -> {
            invocation.<WmProductSales>getArgument(0).setSalesId(100L);
            return 1;
        });

        linkageService.createDraft(notice(), Collections.singletonList(line()), "tester");

        ArgumentCaptor<WmProductSalesLine> captor = ArgumentCaptor.forClass(WmProductSalesLine.class);
        verify(salesLineService).insertWmProductSalesLine(captor.capture());
        assertEquals(100L, captor.getValue().getSalesId());
        assertEquals(new BigDecimal("6"), captor.getValue().getQuantitySales());
        assertEquals("AUTO:SALES_LINE:NOTICE:10", captor.getValue().getAttr1());
        verify(menuNoticeService).markUnread(MenuLinkageNoticePaths.PRODUCT_SALES);
    }

    @Test
    void repeatedConfirmationDoesNotCreateAnotherNotice() {
        when(salesService.selectWmProductSalesList(any(WmProductSales.class)))
                .thenReturn(Collections.singletonList(new WmProductSales()));

        linkageService.createDraft(notice(), Collections.singletonList(line()), "tester");

        verify(salesService, never()).insertWmProductSales(any(WmProductSales.class));
        verify(menuNoticeService, never()).markUnread(MenuLinkageNoticePaths.PRODUCT_SALES);
    }

    private WmSalesNotice notice() {
        WmSalesNotice notice = new WmSalesNotice();
        notice.setNoticeId(1L);
        notice.setNoticeCode("SN-001");
        notice.setSoCode("SO-001");
        notice.setClientId(2L);
        notice.setClientCode("C-001");
        notice.setClientName("客户");
        return notice;
    }

    private WmSalesNoticeLine line() {
        WmSalesNoticeLine line = new WmSalesNoticeLine();
        line.setLineId(10L);
        line.setNoticeId(1L);
        line.setItemId(3L);
        line.setItemCode("FG-001");
        line.setItemName("成品");
        line.setUnitOfMeasure("PCS");
        line.setQuantitySales(new BigDecimal("6"));
        line.setOqcCheck(UserConstants.YES);
        return line;
    }
}
