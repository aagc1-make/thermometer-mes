package com.ktg.mes;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.mapper.*;
import com.ktg.mes.wm.service.impl.IssueAutoPickService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IssueAutoPickServiceTest {
    private final PostingLockMapper locks = mock(PostingLockMapper.class);
    private final WmIssueHeaderMapper headers = mock(WmIssueHeaderMapper.class);
    private final WmIssueLineMapper lines = mock(WmIssueLineMapper.class);
    private final WmIssueDetailMapper details = mock(WmIssueDetailMapper.class);
    private final WmMaterialStockMapper stocks = mock(WmMaterialStockMapper.class);
    private IssueAutoPickService service;
    private WmIssueHeader header;

    @Before public void setup() {
        service = new IssueAutoPickService(locks, headers, lines, details, stocks);
        header = new WmIssueHeader(); header.setIssueId(1L); header.setStatus("APPROVING");
        when(locks.issue(1L)).thenReturn("APPROVING");
        when(headers.selectWmIssueHeaderByIssueId(1L)).thenReturn(header);
        when(details.selectWmIssueDetailList(any())).thenReturn(Collections.emptyList());
        when(details.insertWmIssueDetail(any())).thenReturn(1);
        when(headers.updateWmIssueHeader(any())).thenReturn(1);
    }

    @Test public void splitsFifoBatchesAndCopiesKnownFieldsWithoutClientInput() {
        when(lines.selectWmIssueLineList(any())).thenReturn(Arrays.asList(line(1, "10")));
        WmMaterialStock old = stock(7, "4", 100), newer = stock(8, "20", 200);
        when(stocks.selectIssuePickStocks(3L)).thenReturn(Arrays.asList(newer, old));
        service.pick(1L, "tester");
        ArgumentCaptor<WmIssueDetail> saved = ArgumentCaptor.forClass(WmIssueDetail.class);
        verify(details, times(2)).insertWmIssueDetail(saved.capture());
        assertEquals("7", saved.getAllValues().get(0).getMaterialStockId());
        assertEquals(new BigDecimal("4"), saved.getAllValues().get(0).getQuantity());
        assertEquals(new BigDecimal("6"), saved.getAllValues().get(1).getQuantity());
        WmIssueDetail d = saved.getValue();
        assertEquals("B8", d.getBatchCode()); assertEquals("原料仓", d.getWarehouseName());
        assertEquals("A01", d.getAreaCode()); assertEquals("tester", d.getCreateBy());
        ArgumentCaptor<WmIssueHeader> updated = ArgumentCaptor.forClass(WmIssueHeader.class);
        verify(headers).updateWmIssueHeader(updated.capture());
        assertEquals("APPROVED", updated.getValue().getStatus());
        verify(locks).inventory();
    }

    @Test public void shortageOnLastLineDoesNotSaveEarlierPlansOrChangeStatus() {
        WmIssueLine second = line(2, "8"); second.setItemId(4L); second.setItemCode("缺料B");
        when(lines.selectWmIssueLineList(any())).thenReturn(Arrays.asList(line(1, "5"), second));
        when(stocks.selectIssuePickStocks(3L)).thenReturn(Arrays.asList(stock(7, "20", 100)));
        when(stocks.selectIssuePickStocks(4L)).thenReturn(Collections.emptyList());
        try { service.pick(1L, "tester"); fail("must reject shortage"); }
        catch (ServiceException e) { assertTrue(e.getMessage().contains("缺料B")); assertTrue(e.getMessage().contains("8")); }
        verify(details, never()).insertWmIssueDetail(any());
        verify(headers, never()).updateWmIssueHeader(any());
    }

    @Test public void sameStockCannotBeOverallocatedAcrossTwoLines() {
        when(lines.selectWmIssueLineList(any())).thenReturn(Arrays.asList(line(1, "6"), line(2, "6")));
        when(stocks.selectIssuePickStocks(3L)).thenReturn(Arrays.asList(stock(7, "10", 100)));
        try { service.pick(1L, "tester"); fail("shared stock shortage"); }
        catch (ServiceException e) { assertTrue(e.getMessage().contains("2")); }
        verify(details, never()).insertWmIssueDetail(any());
    }

    @Test public void existingPartialPickIsKeptAndOnlyRemainingDemandIsFilled() {
        when(lines.selectWmIssueLineList(any())).thenReturn(Arrays.asList(line(1, "10")));
        WmMaterialStock s = stock(7, "10", 100);
        when(stocks.selectIssuePickStocks(3L)).thenReturn(Arrays.asList(s));
        WmIssueDetail existing = existing(s, "4");
        when(details.selectWmIssueDetailList(any())).thenReturn(Arrays.asList(existing));
        service.pick(1L, "tester");
        ArgumentCaptor<WmIssueDetail> saved = ArgumentCaptor.forClass(WmIssueDetail.class);
        verify(details).insertWmIssueDetail(saved.capture());
        assertEquals(new BigDecimal("6"), saved.getValue().getQuantity());
        verify(details, never()).deleteWmIssueDetailByDetailId(any());
    }

    @Test public void retryOfAlreadyCompletePickDoesNotDuplicateDetails() {
        when(locks.issue(1L)).thenReturn("APPROVED"); header.setStatus("APPROVED");
        when(lines.selectWmIssueLineList(any())).thenReturn(Arrays.asList(line(1, "10")));
        WmMaterialStock s = stock(7, "10", 100);
        when(stocks.selectIssuePickStocks(3L)).thenReturn(Arrays.asList(s));
        when(details.selectWmIssueDetailList(any())).thenReturn(Arrays.asList(existing(s, "10")));
        service.pick(1L, "tester");
        verify(details, never()).insertWmIssueDetail(any());
        verify(headers, never()).updateWmIssueHeader(any());
    }

    @Test public void respectsSpecifiedBatchAndSkipsFrozenOrVirtualStock() {
        WmIssueLine l = line(1, "1"); l.setBatchId(8L);
        when(lines.selectWmIssueLineList(any())).thenReturn(Arrays.asList(l));
        WmMaterialStock wrongBatch = stock(7, "10", 100), right = stock(8, "10", 200);
        WmMaterialStock frozen = stock(9, "10", 50); frozen.setBatchId(8L); frozen.setFrozenFlag("Y");
        WmMaterialStock virtual = stock(10, "10", 30); virtual.setBatchId(8L); virtual.setWarehouseCode("VIRTUAL_WH");
        WmMaterialStock other = stock(11, "10", 20); other.setBatchId(8L); other.setItemId(4L);
        when(stocks.selectIssuePickStocks(3L)).thenReturn(Arrays.asList(wrongBatch, frozen, virtual, other, right));
        service.pick(1L, "tester");
        ArgumentCaptor<WmIssueDetail> saved = ArgumentCaptor.forClass(WmIssueDetail.class);
        verify(details).insertWmIssueDetail(saved.capture());
        assertEquals("8", saved.getValue().getMaterialStockId());
    }

    @Test public void decimalQuantitiesAreExact() {
        when(lines.selectWmIssueLineList(any())).thenReturn(Arrays.asList(line(1, "0.3")));
        when(stocks.selectIssuePickStocks(3L)).thenReturn(Arrays.asList(stock(7, "0.1", 100), stock(8, "0.2", 200)));
        service.pick(1L, "tester");
        ArgumentCaptor<WmIssueDetail> saved = ArgumentCaptor.forClass(WmIssueDetail.class);
        verify(details, times(2)).insertWmIssueDetail(saved.capture());
        assertEquals(new BigDecimal("0.2"), saved.getValue().getQuantity());
    }

    @Test public void finishedAndCanceledOrdersCannotBePicked() {
        for (String status : Arrays.asList("FINISHED", "CANCELED")) {
            when(locks.issue(1L)).thenReturn(status);
            try { service.pick(1L, "tester"); fail("must reject " + status); } catch (ServiceException expected) { }
        }
        verifyNoInteractions(stocks, details);
    }

    private WmIssueLine line(long id, String qty) {
        WmIssueLine l = new WmIssueLine(); l.setIssueId(1L); l.setLineId(id); l.setItemId(3L);
        l.setItemCode("TG-ST-001"); l.setItemName("上壳"); l.setUnitName("个");
        l.setUnitOfMeasure("个"); l.setQuantityIssued(new BigDecimal(qty)); return l;
    }
    private WmMaterialStock stock(long id, String qty, long date) {
        WmMaterialStock s = new WmMaterialStock(); s.setMaterialStockId(id); s.setItemId(3L);
        s.setBatchId(id); s.setBatchCode("B" + id); s.setWarehouseId(20L); s.setWarehouseCode("RAW");
        s.setWarehouseName("原料仓"); s.setLocationId(21L); s.setLocationCode("L01");
        s.setAreaId(22L); s.setAreaCode("A01"); s.setFrozenFlag("N");
        s.setQuantityOnhand(new BigDecimal(qty)); s.setRecptDate(new Date(date)); return s;
    }
    private WmIssueDetail existing(WmMaterialStock s, String qty) {
        WmIssueDetail d = new WmIssueDetail(); d.setDetailId(1L); d.setIssueId(1L); d.setLineId(1L); d.setItemId(3L);
        d.setMaterialStockId(String.valueOf(s.getMaterialStockId())); d.setQuantity(new BigDecimal(qty));
        d.setBatchId(s.getBatchId()); d.setBatchCode(s.getBatchCode()); d.setWarehouseId(s.getWarehouseId());
        d.setWarehouseCode(s.getWarehouseCode()); d.setLocationId(s.getLocationId()); d.setLocationCode(s.getLocationCode());
        d.setAreaId(s.getAreaId()); d.setAreaCode(s.getAreaCode()); return d;
    }
}
