package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.domain.WmProductRecpt;
import com.ktg.mes.wm.domain.WmProductRecptLine;
import com.ktg.mes.wm.mapper.ProductReceiptGuardMapper;
import com.ktg.mes.wm.service.IWmProductRecptLineService;
import com.ktg.mes.wm.service.IWmProductRecptService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 产品入库联动（工单完工 → 产品入库草稿）：按末工序合格产出 + 来源批次，移除计划量兜底。
 */
class ProductReceiptLinkageServiceTest {

    private IProWorkorderService workorderService;
    private IWmProductRecptService recptService;
    private IWmProductRecptLineService recptLineService;
    private ProductReceiptGuardMapper guardMapper;
    private AutoCodeUtil autoCodeUtil;
    private MenuLinkageNoticeService menuNoticeService;
    private ProductReceiptLinkageService linkageService;

    @BeforeEach
    void setUp() {
        workorderService = mock(IProWorkorderService.class);
        recptService = mock(IWmProductRecptService.class);
        recptLineService = mock(IWmProductRecptLineService.class);
        guardMapper = mock(ProductReceiptGuardMapper.class);
        autoCodeUtil = mock(AutoCodeUtil.class);
        menuNoticeService = mock(MenuLinkageNoticeService.class);
        linkageService = new ProductReceiptLinkageService(workorderService, recptService, recptLineService,
                guardMapper, autoCodeUtil, menuNoticeService);
        // 默认：无旧口径草稿、无已建批次草稿
        when(guardMapper.countProductRecptByLinkageKey(anyString())).thenReturn(0);
        when(guardMapper.terminalProcessId(any())).thenReturn(9409L);
        when(guardMapper.totalQualifiedOutput(any(), any(), any())).thenReturn(new BigDecimal("10"));
        when(guardMapper.completedPackagingQuantity(any(), any())).thenReturn(new BigDecimal("10"));
        when(guardMapper.finishedProductRecptTotal(any())).thenReturn(BigDecimal.ZERO);
        when(guardMapper.countFinishedFeedbackAtProcess(any(), any())).thenReturn(1);
        when(guardMapper.produceProcessesForProduct(any(), any())).thenReturn(java.util.Collections.singletonList(9407L));
        when(guardMapper.finishedProductRecptByBatch(any(), anyString())).thenReturn(BigDecimal.ZERO);
        when(guardMapper.occupiedProductRecptByBatch(any(), anyString(), any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void doesNotCreateForUnfinishedWorkorder() {
        ProWorkorder workorder = workorder(UserConstants.ORDER_STATUS_CONFIRMED, 0L);
        when(workorderService.selectProWorkorderByWorkorderId(1L)).thenReturn(workorder);

        assertNull(linkageService.ensureDraftForFinishedWorkorder(1L, "tester"));
        verify(recptService, never()).insertWmProductRecpt(any(WmProductRecpt.class));
    }

    @Test
    void doesNotCreateForSemiFinishedSubWorkorder() {
        ProWorkorder workorder = workorder(UserConstants.ORDER_STATUS_FINISHED, 100L);
        when(workorderService.selectProWorkorderByWorkorderId(1L)).thenReturn(workorder);

        assertNull(linkageService.ensureDraftForFinishedWorkorder(1L, "tester"));
        verify(recptService, never()).insertWmProductRecpt(any(WmProductRecpt.class));
    }

    @Test
    void doesNotCreateWithoutReliableQualifiedOutput() {
        // 移除计划量兜底：无合格产出 → 不建单
        ProWorkorder workorder = workorder(UserConstants.ORDER_STATUS_FINISHED, 0L);
        workorder.setQuantityProduced(new BigDecimal("7"));
        when(workorderService.selectProWorkorderByWorkorderId(1L)).thenReturn(workorder);
        when(guardMapper.qualifiedOutputByBatch(1L, 9201L, 9407L)).thenReturn(Collections.emptyList());

        assertNull(linkageService.ensureDraftForFinishedWorkorder(1L, "tester"));
        verify(recptService, never()).insertWmProductRecpt(any(WmProductRecpt.class));
    }

    @Test
    void createsDraftPerBatchFromQualifiedOutput() {
        ProWorkorder workorder = workorder(UserConstants.ORDER_STATUS_FINISHED, 0L);
        when(workorderService.selectProWorkorderByWorkorderId(1L)).thenReturn(workorder);
        when(guardMapper.qualifiedOutputByBatch(1L, 9201L, 9407L))
                .thenReturn(Collections.singletonList(batch("B1", new BigDecimal("10"))));
        when(autoCodeUtil.genSerialCode(UserConstants.PRODUCTRECPT_CODE, "")).thenReturn("PR-001");
        when(recptService.insertWmProductRecpt(any(WmProductRecpt.class))).thenAnswer(invocation -> {
            invocation.<WmProductRecpt>getArgument(0).setRecptId(900L);
            return 1;
        });

        WmProductRecpt recpt = linkageService.ensureDraftForFinishedWorkorder(1L, "tester");

        assertEquals("PR-001", recpt.getRecptCode());
        assertEquals(UserConstants.ORDER_STATUS_PREPARE, recpt.getStatus());
        assertEquals("AUTO:PRODUCT_RECPT:WORKORDER:1:BATCH:B1", recpt.getAttr1());
        ArgumentCaptor<WmProductRecptLine> captor = ArgumentCaptor.forClass(WmProductRecptLine.class);
        verify(recptLineService).insertWmProductRecptLine(captor.capture());
        assertEquals(0, new BigDecimal("10").compareTo(captor.getValue().getQuantityRecived()));
        assertEquals("B1", captor.getValue().getBatchCode());
        verify(menuNoticeService).markUnread(MenuLinkageNoticePaths.PRODUCT_RECPT);
    }

    @Test
    void deductsFinishedAndOccupiedFromAvailable() {
        ProWorkorder workorder = workorder(UserConstants.ORDER_STATUS_FINISHED, 0L);
        when(workorderService.selectProWorkorderByWorkorderId(1L)).thenReturn(workorder);
        when(guardMapper.qualifiedOutputByBatch(1L, 9201L, 9407L))
                .thenReturn(Collections.singletonList(batch("B1", new BigDecimal("10"))));
        when(guardMapper.finishedProductRecptByBatch(1L, "B1")).thenReturn(new BigDecimal("6"));
        when(guardMapper.occupiedProductRecptByBatch(1L, "B1", null)).thenReturn(new BigDecimal("2"));
        when(autoCodeUtil.genSerialCode(anyString(), anyString())).thenReturn("PR-002");
        when(recptService.insertWmProductRecpt(any(WmProductRecpt.class))).thenAnswer(invocation -> {
            invocation.<WmProductRecpt>getArgument(0).setRecptId(901L);
            return 1;
        });

        linkageService.ensureDraftForFinishedWorkorder(1L, "tester");

        ArgumentCaptor<WmProductRecptLine> captor = ArgumentCaptor.forClass(WmProductRecptLine.class);
        verify(recptLineService).insertWmProductRecptLine(captor.capture());
        assertEquals(0, new BigDecimal("2").compareTo(captor.getValue().getQuantityRecived()));
    }

    private Map<String, Object> batch(String code, BigDecimal quantity) {
        Map<String, Object> map = new HashMap<>();
        map.put("batchCode", code);
        map.put("batchId", null);
        map.put("quantity", quantity);
        return map;
    }

    private ProWorkorder workorder(String status, Long parentId) {
        ProWorkorder workorder = new ProWorkorder();
        workorder.setWorkorderId(1L);
        workorder.setWorkorderCode("WO-001");
        workorder.setWorkorderName("红外测温枪");
        workorder.setProductId(9201L);
        workorder.setProductCode("TG-FG-001");
        workorder.setProductName("红外测温枪整机");
        workorder.setUnitOfMeasure("PCS");
        workorder.setQuantity(new BigDecimal("10"));
        workorder.setQuantityProduced(new BigDecimal("10"));
        workorder.setStatus(status);
        workorder.setParentId(parentId);
        return workorder;
    }
}
