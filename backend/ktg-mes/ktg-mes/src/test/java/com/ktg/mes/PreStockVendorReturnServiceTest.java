package com.ktg.mes;

import com.ktg.mes.qc.domain.QcIqc;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmRtVendor;
import com.ktg.mes.wm.domain.WmRtVendorLine;
import com.ktg.mes.wm.mapper.VendorReturnGuardMapper;
import com.ktg.mes.wm.service.IWmRtVendorLineService;
import com.ktg.mes.wm.service.IWmRtVendorService;
import com.ktg.mes.wm.service.impl.PreStockVendorReturnService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 未入库退供：部分退供、取消重建、累计守恒。
 */
public class PreStockVendorReturnServiceTest {
    private IWmRtVendorService headers;
    private IWmRtVendorLineService lines;
    private VendorReturnGuardMapper guards;
    private PreStockVendorReturnService service;
    private QcIqc iqc;
    private WmArrivalNotice notice;
    private WmArrivalNoticeLine line;
    private final AtomicLong ids = new AtomicLong(100);

    @Before public void setup() {
        headers = mock(IWmRtVendorService.class);
        lines = mock(IWmRtVendorLineService.class);
        guards = mock(VendorReturnGuardMapper.class);
        AutoCodeUtil codes = mock(AutoCodeUtil.class);
        when(codes.genSerialCode(anyString(), isNull())).thenReturn("RT-1");
        when(lines.insertWmRtVendorLine(any())).thenReturn(1);
        doAnswer(inv -> { ((WmRtVendor) inv.getArgument(0)).setRtId(ids.incrementAndGet()); return 1; })
                .when(headers).insertWmRtVendor(any());
        service = new PreStockVendorReturnService(headers, lines, guards, codes);

        iqc = new QcIqc();
        iqc.setIqcId(1L); iqc.setSourceDocId(2L); iqc.setSourceLineId(3L); iqc.setItemId(4L);
        iqc.setQuantityUnqualified(new BigDecimal("10"));
        notice = new WmArrivalNotice(); notice.setNoticeId(2L); notice.setVendorId(5L);
        line = new WmArrivalNoticeLine(); line.setNoticeId(2L); line.setLineId(3L); line.setItemId(4L); line.setItemCode("I");
    }

    /** 首次不合格 10，创建草稿数量应取不合格数量（无已执行）。 */
    @Test public void firstDraftTakesUnqualifiedQuantity() {
        when(guards.openPreStockDraft(1L)).thenReturn(null);
        when(guards.finishedPreStockReturned(1L, -1L)).thenReturn(BigDecimal.ZERO);
        Long id = service.create(iqc, notice, line, new BigDecimal("10"), "operator");
        assertNotNull(id);
        ArgumentCaptor<WmRtVendorLine> item = ArgumentCaptor.forClass(WmRtVendorLine.class);
        verify(lines).insertWmRtVendorLine(item.capture());
        assertEquals(0, new BigDecimal("10").compareTo(item.getValue().getQuantityRted()));
        assertEquals("PRE_STOCK", ((WmRtVendor) argOf(headers)).getAttr2());
    }

    /** 已执行退供 4 后，剩余可退 6：新草稿只能取 6（不能再次取 10）。 */
    @Test public void partialReturnDraftTakesRemainingQuantity() {
        when(guards.openPreStockDraft(1L)).thenReturn(null);
        when(guards.finishedPreStockReturned(1L, -1L)).thenReturn(new BigDecimal("4"));
        service.create(iqc, notice, line, new BigDecimal("10"), "operator");
        ArgumentCaptor<WmRtVendorLine> item = ArgumentCaptor.forClass(WmRtVendorLine.class);
        verify(lines).insertWmRtVendorLine(item.capture());
        assertEquals(0, new BigDecimal("6").compareTo(item.getValue().getQuantityRted()));
    }

    /** 已执行累计等于不合格数量：不再生成草稿。 */
    @Test public void fullyReturnedCreatesNoDraft() {
        when(guards.openPreStockDraft(1L)).thenReturn(null);
        when(guards.finishedPreStockReturned(1L, -1L)).thenReturn(new BigDecimal("10"));
        assertNull(service.create(iqc, notice, line, new BigDecimal("10"), "operator"));
        verify(headers, never()).insertWmRtVendor(any());
    }

    /** 已有进行中草稿时复用，不重复建单（取消/删除后才允许重建）。 */
    @Test public void openDraftIsReused() {
        when(guards.openPreStockDraft(1L)).thenReturn(77L);
        assertEquals(Long.valueOf(77L), service.create(iqc, notice, line, new BigDecimal("10"), "operator"));
        verify(headers, never()).insertWmRtVendor(any());
    }

    /** 请求数量超过剩余可退时按剩余可退截断，不超退。 */
    @Test public void requestedQuantityIsCappedByRemaining() {
        when(guards.openPreStockDraft(1L)).thenReturn(null);
        when(guards.finishedPreStockReturned(1L, -1L)).thenReturn(new BigDecimal("7"));
        service.create(iqc, notice, line, new BigDecimal("10"), "operator");
        ArgumentCaptor<WmRtVendorLine> item = ArgumentCaptor.forClass(WmRtVendorLine.class);
        verify(lines).insertWmRtVendorLine(item.capture());
        assertEquals(0, new BigDecimal("3").compareTo(item.getValue().getQuantityRted()));
    }

    private Object argOf(IWmRtVendorService svc) {
        ArgumentCaptor<WmRtVendor> c = ArgumentCaptor.forClass(WmRtVendor.class);
        verify(svc, atLeastOnce()).insertWmRtVendor(c.capture());
        return c.getValue();
    }
}
