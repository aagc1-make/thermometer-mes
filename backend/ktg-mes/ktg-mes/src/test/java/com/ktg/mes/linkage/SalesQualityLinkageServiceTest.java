package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.qc.domain.QcOqc;
import com.ktg.mes.qc.domain.QcTemplateIndex;
import com.ktg.mes.qc.domain.QcTemplateProduct;
import com.ktg.mes.qc.service.IQcOqcLineService;
import com.ktg.mes.qc.service.IQcOqcService;
import com.ktg.mes.qc.service.IQcTemplateIndexService;
import com.ktg.mes.qc.service.IQcTemplateProductService;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesLine;
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

class SalesQualityLinkageServiceTest {

    private IQcOqcService oqcService;
    private IQcTemplateProductService templateProductService;
    private IQcTemplateIndexService templateIndexService;
    private IQcOqcLineService oqcLineService;
    private AutoCodeUtil autoCodeUtil;
    private MenuLinkageNoticeService menuNoticeService;
    private SalesQualityLinkageService linkageService;

    @BeforeEach
    void setUp() {
        oqcService = mock(IQcOqcService.class);
        templateProductService = mock(IQcTemplateProductService.class);
        templateIndexService = mock(IQcTemplateIndexService.class);
        oqcLineService = mock(IQcOqcLineService.class);
        autoCodeUtil = mock(AutoCodeUtil.class);
        menuNoticeService = mock(MenuLinkageNoticeService.class);
        linkageService = new SalesQualityLinkageService(oqcService, templateProductService,
                templateIndexService, oqcLineService, autoCodeUtil, menuNoticeService);
    }

    @Test
    void createsOneOqcDraftForRequiredSalesLine() {
        QcTemplateProduct template = new QcTemplateProduct();
        template.setTemplateId(7L);
        QcTemplateIndex index = new QcTemplateIndex();
        index.setIndexId(8L);
        when(oqcService.selectQcOqcList(any(QcOqc.class))).thenReturn(Collections.emptyList());
        when(templateProductService.selectQcTemplateProductList(any(QcTemplateProduct.class)))
                .thenReturn(Collections.singletonList(template));
        when(templateIndexService.selectQcTemplateIndexList(any(QcTemplateIndex.class)))
                .thenReturn(Collections.singletonList(index));
        when(autoCodeUtil.genSerialCode(UserConstants.QC_OQC_CODE, null)).thenReturn("OQC-001");
        when(oqcService.insertQcOqc(any(QcOqc.class))).thenAnswer(invocation -> {
            invocation.<QcOqc>getArgument(0).setOqcId(99L);
            return 1;
        });

        linkageService.createDrafts(sales(), Collections.singletonList(line()), "tester");

        ArgumentCaptor<QcOqc> captor = ArgumentCaptor.forClass(QcOqc.class);
        verify(oqcService).insertQcOqc(captor.capture());
        assertEquals(UserConstants.SOURCE_DOC_TYPE_PRODUCT_SALSE, captor.getValue().getSourceDocType());
        assertEquals(new BigDecimal("6"), captor.getValue().getQuantityCheck());
        assertEquals(BigDecimal.ZERO, captor.getValue().getQuantityQualified());
        verify(oqcLineService).insertQcOqcLine(any());
        verify(menuNoticeService).markUnread(MenuLinkageNoticePaths.OQC);
    }

    @Test
    void repeatedSalesSubmissionDoesNotCreateAnotherOqc() {
        when(oqcService.selectQcOqcList(any(QcOqc.class)))
                .thenReturn(Collections.singletonList(new QcOqc()));

        linkageService.createDrafts(sales(), Collections.singletonList(line()), "tester");

        verify(oqcService, never()).insertQcOqc(any(QcOqc.class));
        verify(menuNoticeService, never()).markUnread(MenuLinkageNoticePaths.OQC);
    }

    private WmProductSales sales() {
        WmProductSales sales = new WmProductSales();
        sales.setSalesId(1L);
        sales.setSalesCode("SO-001");
        sales.setClientId(2L);
        sales.setClientCode("C-001");
        sales.setClientName("客户");
        return sales;
    }

    private WmProductSalesLine line() {
        WmProductSalesLine line = new WmProductSalesLine();
        line.setLineId(10L);
        line.setSalesId(1L);
        line.setOqcCheck(UserConstants.YES);
        line.setItemId(3L);
        line.setItemCode("FG-001");
        line.setItemName("成品");
        line.setUnitOfMeasure("PCS");
        line.setQuantitySales(new BigDecimal("6"));
        return line;
    }
}
