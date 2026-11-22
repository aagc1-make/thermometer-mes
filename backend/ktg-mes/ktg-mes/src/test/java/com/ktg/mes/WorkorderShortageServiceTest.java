package com.ktg.mes;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.wm.domain.WmMaterialStock;
import com.ktg.mes.wm.domain.WmTransfer;
import com.ktg.mes.wm.domain.WmTransferLine;
import com.ktg.mes.wm.domain.vo.WorkorderShortagePlan;
import com.ktg.mes.wm.domain.vo.WorkorderShortagePlan.*;
import com.ktg.mes.wm.mapper.WorkorderShortageMapper;
import com.ktg.mes.wm.service.IWmTransferLineService;
import com.ktg.mes.wm.service.IWmTransferService;
import com.ktg.mes.wm.service.impl.WorkorderShortageService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WorkorderShortageServiceTest {
    private final WorkorderShortageMapper mapper = mock(WorkorderShortageMapper.class);
    private final IWmTransferService headers = mock(IWmTransferService.class);
    private final IWmTransferLineService lines = mock(IWmTransferLineService.class);
    private final AutoCodeUtil codes = mock(AutoCodeUtil.class);
    private final WorkorderShortageService service = new WorkorderShortageService(mapper, headers, lines, codes);
    private final ProWorkorder order = new ProWorkorder();
    private final WmMaterialStock first = stock(101, 8);
    private final WmMaterialStock second = stock(102, 20);

    @Before public void setup() {
        order.setWorkorderId(7L);
        order.setWorkorderCode("WO-7");
        order.setStatus("CONFIRMED");
        when(mapper.order(eq(7L), anyBoolean())).thenReturn(order);
        when(mapper.materials(7L, "WO-7")).thenAnswer(inv -> {
            Material material = new Material();
            material.itemId = 2L;
            material.required = BigDecimal.valueOf(100);
            material.issued = BigDecimal.valueOf(30);
            material.onHand = BigDecimal.valueOf(50);
            return Collections.singletonList(material);
        });
        when(mapper.sources(Collections.singletonList(2L))).thenReturn(Arrays.asList(first, second));
    }

    @Test public void allocatesOnlyTheUncoveredAmountAndReportsTheResidual() {
        second.setQuantityOnhand(BigDecimal.valueOf(4));
        Material row = service.analyze(7L).materials.get(0);
        assertEquals(BigDecimal.valueOf(20), row.missing);
        assertEquals(BigDecimal.valueOf(8), row.uncovered);
        assertEquals(BigDecimal.valueOf(8), row.sources.get(0).quantity);
        assertEquals(BigDecimal.valueOf(4), row.sources.get(1).quantity);
        verifyNoInteractions(headers, lines);
    }

    @Test public void persistsOnlyCheckedStockAndReplaysTheExactOperation() {
        Confirmation confirmation = selectSecond();
        when(headers.insertWmTransfer(any())).thenAnswer(inv -> {
            inv.<WmTransfer>getArgument(0).setTransferId(42L);
            return 1;
        });
        when(lines.insertWmTransferLine(any())).thenReturn(1);
        Document doc = service.confirm(7L, confirmation, "planner");
        ArgumentCaptor<WmTransfer> header = ArgumentCaptor.forClass(WmTransfer.class);
        ArgumentCaptor<WmTransferLine> line = ArgumentCaptor.forClass(WmTransferLine.class);
        verify(headers).insertWmTransfer(header.capture());
        verify(headers).checkUnique(header.getValue());
        verify(lines).insertWmTransferLine(line.capture());
        assertEquals("PREPARE", doc.status);
        assertEquals("WH-RAW", header.getValue().getDestination());
        assertEquals(Long.valueOf(102), line.getValue().getMaterialStockId());
        assertEquals(BigDecimal.valueOf(12), line.getValue().getQuantityTransfer());
        doc.operation = header.getValue().getAttr2();
        when(mapper.documents("WO-SHORTAGE/7")).thenReturn(Collections.singletonList(doc));
        assertSame(doc, service.confirm(7L, confirmation, "planner"));
        verifyNoMoreInteractions(headers, lines);
    }

    @Test public void rechecksStockAndRejectsChangedEvidenceBeforeWriting() {
        Confirmation confirmation = selectSecond();
        second.setQuantityOnhand(BigDecimal.ONE);
        assertThrows(ServiceException.class, () -> service.confirm(7L, confirmation, "planner"));
        verifyNoInteractions(headers, lines);
    }

    @Test public void blocksAnActiveTransferAndAClosedWorkorder() {
        Confirmation confirmation = selectSecond();
        Document open = new Document();
        open.status = "UNSTOCK";
        when(mapper.documents("WO-SHORTAGE/7")).thenReturn(Collections.singletonList(open));
        assertThrows(ServiceException.class, () -> service.confirm(7L, confirmation, "planner"));
        open.status = "CANCELED";
        order.setStatus("FINISHED");
        assertThrows(ServiceException.class, () -> service.confirm(7L, confirmation, "planner"));
        verifyNoInteractions(headers, lines);
    }

    private Confirmation selectSecond() {
        WorkorderShortagePlan plan = service.analyze(7L);
        Source source = plan.materials.get(0).sources.get(1);
        Selection selection = new Selection();
        selection.stockId = source.stockId;
        selection.version = source.version;
        Confirmation request = new Confirmation();
        request.requestId = plan.requestId;
        request.lines = Collections.singletonList(selection);
        return request;
    }

    private static WmMaterialStock stock(long id, int qty) {
        WmMaterialStock stock = new WmMaterialStock();
        stock.setMaterialStockId(id);
        stock.setItemId(2L);
        stock.setQuantityOnhand(BigDecimal.valueOf(qty));
        return stock;
    }
}
