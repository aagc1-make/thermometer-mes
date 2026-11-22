package com.ktg.mes;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.pro.domain.ProFeedback;
import com.ktg.mes.pro.domain.ProRoute;
import com.ktg.mes.pro.domain.ProRouteProductBom;
import com.ktg.mes.pro.mapper.ProRouteMapper;
import com.ktg.mes.pro.mapper.ProRouteProductBomMapper;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.mapper.*;
import com.ktg.mes.wm.service.impl.WmItemConsumeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WmItemConsumeBatchScopeTest {
    private final WmItemConsumeServiceImpl service = new WmItemConsumeServiceImpl();
    private final WmItemConsumeMapper headers = mock(WmItemConsumeMapper.class);
    private final WmItemConsumeLineMapper lines = mock(WmItemConsumeLineMapper.class);
    private final WmItemConsumeDetailMapper details = mock(WmItemConsumeDetailMapper.class);
    private final WmMaterialStockMapper stocks = mock(WmMaterialStockMapper.class);
    private ProFeedback feedback;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "wmItemConsumeMapper", headers);
        ReflectionTestUtils.setField(service, "wmItemConsumeLineMapper", lines);
        ReflectionTestUtils.setField(service, "wmItemConsumeDetailMapper", details);
        ReflectionTestUtils.setField(service, "wmMaterialStockMapper", stocks);
        ProRouteMapper routes = mock(ProRouteMapper.class);
        ProRouteProductBomMapper boms = mock(ProRouteProductBomMapper.class);
        ReflectionTestUtils.setField(service, "proRouteMapper", routes);
        ReflectionTestUtils.setField(service, "proRouteProductBomMapper", boms);
        ProRoute route = new ProRoute(); route.setRouteId(20L);
        when(routes.getRouteByProductId(10L)).thenReturn(route);
        ProRouteProductBom bom = new ProRouteProductBom();
        bom.setItemId(30L); bom.setItemCode("RM-30"); bom.setQuantity(BigDecimal.ONE);
        when(boms.selectProRouteProductBomList(any())).thenReturn(Collections.singletonList(bom));
        WmWarehouseMapper wh = mock(WmWarehouseMapper.class);
        WmStorageLocationMapper loc = mock(WmStorageLocationMapper.class);
        WmStorageAreaMapper area = mock(WmStorageAreaMapper.class);
        ReflectionTestUtils.setField(service, "wmWarehouseMapper", wh);
        ReflectionTestUtils.setField(service, "wmStorageLocationMapper", loc);
        ReflectionTestUtils.setField(service, "wmStorageAreaMapper", area);
        WmWarehouse warehouse = new WmWarehouse(); warehouse.setWarehouseId(1L);
        WmStorageLocation location = new WmStorageLocation(); location.setLocationId(2L);
        WmStorageArea storageArea = new WmStorageArea(); storageArea.setAreaId(3L);
        when(wh.selectWmWarehouseByWarehouseCode(any())).thenReturn(warehouse);
        when(loc.selectWmStorageLocationByLocationCode(any())).thenReturn(location);
        when(area.selectWmStorageAreaByAreaCode(any())).thenReturn(storageArea);
        feedback = new ProFeedback(); feedback.setItemId(10L); feedback.setWorkorderId(40L);
        feedback.setProcessId(50L); feedback.setQuantityFeedback(new BigDecimal("10"));
    }

    @Test
    void constrainedConsumptionExcludesOlderStockEvenWhenItIsFirst() {
        scope();
        when(stocks.selectWmMaterialStockList(any())).thenReturn(Arrays.asList(
                stock(1L, "HISTORY", "100"), stock(2L, "THIS-RUN", "10")));
        service.generateItemConsume(feedback);
        List<WmItemConsumeDetail> consumed = consumed();
        assertEquals(1, consumed.size());
        assertEquals("THIS-RUN", consumed.get(0).getBatchCode());
        assertEquals(2L, consumed.get(0).getMaterialStockId());
        assertEquals(new BigDecimal("10"), consumed.get(0).getQuantity());
    }

    @Test
    void constrainedConsumptionRejectsInsufficientScopeInsteadOfUsingHistoryOrUnbatchedFallback() {
        scope();
        when(stocks.selectWmMaterialStockList(any())).thenReturn(Arrays.asList(
                stock(1L, "HISTORY", "100"), stock(2L, "THIS-RUN", "3")));
        assertThrows(ServiceException.class, () -> service.generateItemConsume(feedback));
        verify(details, never()).insertWmItemConsumeDetail(any());
    }

    @Test
    void constrainedConsumptionRejectsMissingAndNonPositiveStock() {
        scope();
        when(stocks.selectWmMaterialStockList(any())).thenReturn(Arrays.asList(
                stock(2L, "THIS-RUN", "0"), stock(3L, "THIS-RUN", "-1")));
        assertThrows(ServiceException.class, () -> service.generateItemConsume(feedback));
        verify(details, never()).insertWmItemConsumeDetail(any());
    }

    @Test
    void constrainedConsumptionRejectsEmptyStockInsteadOfUnbatchedFallback() {
        scope();
        when(stocks.selectWmMaterialStockList(any())).thenReturn(Collections.emptyList());
        assertThrows(ServiceException.class, () -> service.generateItemConsume(feedback));
        verify(details, never()).insertWmItemConsumeDetail(any());
    }

    @Test
    void constrainedConsumptionRejectsMissingItemScope() {
        feedback.getParams().put("feedbackBatchScope", Collections.emptyMap());
        when(stocks.selectWmMaterialStockList(any())).thenReturn(Collections.singletonList(stock(1L, "HISTORY", "100")));
        assertThrows(ServiceException.class, () -> service.generateItemConsume(feedback));
    }

    @Test
    void originalConsumptionStillUsesExistingFifoBehavior() {
        when(stocks.selectWmMaterialStockList(any())).thenReturn(Collections.singletonList(stock(1L, "HISTORY", "100")));
        service.generateItemConsume(feedback);
        assertEquals("HISTORY", consumed().get(0).getBatchCode());
    }

    @Test
    void originalConsumptionRetainsUnbatchedFallback() {
        when(stocks.selectWmMaterialStockList(any())).thenReturn(Collections.emptyList());
        service.generateItemConsume(feedback);
        assertNull(consumed().get(0).getBatchCode());
    }

    private void scope() {
        feedback.getParams().put("feedbackBatchScope", Collections.singletonMap("30", Collections.singletonList("THIS-RUN")));
    }

    private WmMaterialStock stock(Long id, String batch, String quantity) {
        WmMaterialStock stock = new WmMaterialStock(); stock.setMaterialStockId(id);
        stock.setItemId(30L); stock.setBatchCode(batch); stock.setQuantityOnhand(new BigDecimal(quantity));
        return stock;
    }

    private List<WmItemConsumeDetail> consumed() {
        ArgumentCaptor<WmItemConsumeDetail> capture = ArgumentCaptor.forClass(WmItemConsumeDetail.class);
        verify(details, atLeastOnce()).insertWmItemConsumeDetail(capture.capture());
        return capture.getAllValues();
    }
}
