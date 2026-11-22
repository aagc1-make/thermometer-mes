package com.ktg.mes;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.qc.domain.QcIqc;
import com.ktg.mes.qc.service.IQcIqcService;
import com.ktg.mes.wm.domain.WmRtVendor;
import com.ktg.mes.wm.domain.WmRtVendorLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.VendorReturnGuardMapper;
import com.ktg.mes.wm.mapper.WmMaterialStockMapper;
import com.ktg.mes.wm.service.*;
import com.ktg.mes.wm.service.impl.VendorReturnWorkflowService;
import org.junit.Before;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class VendorReturnWorkflowServiceTest {
    private PostingLockMapper locks; private VendorReturnGuardMapper guards; private IWmRtVendorService headers;
    private IWmRtVendorLineService lines; private IWmRtVendorDetailService details; private IQcIqcService iqcs;
    private IWmItemRecptLineService receiptLines; private WmMaterialStockMapper stocks; private IStorageCoreService storage;
    private VendorReturnWorkflowService service;
    @Before public void setup() {
        locks=mock(PostingLockMapper.class); guards=mock(VendorReturnGuardMapper.class);
        headers=mock(IWmRtVendorService.class); lines=mock(IWmRtVendorLineService.class);
        details=mock(IWmRtVendorDetailService.class); iqcs=mock(IQcIqcService.class);
        receiptLines=mock(IWmItemRecptLineService.class); stocks=mock(WmMaterialStockMapper.class); storage=mock(IStorageCoreService.class);
        service=new VendorReturnWorkflowService(locks,guards,headers,lines,details,iqcs,receiptLines,stocks,storage);
    }
    @Test public void preStockReturnFinishesWithoutInventoryTransaction() {
        when(locks.rtVendor(1L)).thenReturn("APPROVED"); when(guards.lockMode(1L)).thenReturn("PRE_STOCK");
        when(guards.lockSourceKey(1L)).thenReturn("AUTO:RTV:IQC:2"); when(locks.iqc(2L)).thenReturn("FINISHED");
        QcIqc iqc=new QcIqc();iqc.setIqcId(2L);iqc.setSourceDocType("ARRIVAL_NOTICE");iqc.setSourceLineId(3L);
        iqc.setItemId(4L);iqc.setQuantityUnqualified(new BigDecimal("5"));when(iqcs.selectQcIqcByIqcId(2L)).thenReturn(iqc);
        WmRtVendor header=new WmRtVendor();header.setRtId(1L);header.setAttr1("AUTO:RTV:IQC:2");header.setAttr2("PRE_STOCK");
        when(headers.selectWmRtVendorByRtId(1L)).thenReturn(header);when(headers.updateWmRtVendor(header)).thenReturn(1);
        WmRtVendorLine line=new WmRtVendorLine();line.setRtId(1L);line.setItemId(4L);line.setAttr1("IQC:2");line.setAttr2("3");line.setQuantityRted(new BigDecimal("5"));
        when(lines.selectWmRtVendorLineList(any(WmRtVendorLine.class))).thenReturn(Collections.singletonList(line));
        when(details.selectWmRtVendorDetailList(any())).thenReturn(Collections.emptyList());
        service.execute(1L);
        assertEquals("FINISHED",header.getStatus());verifyNoInteractions(storage);verify(headers).updateWmRtVendor(header);
    }
    @Test(expected=ServiceException.class) public void finishedCannotBeExecutedAgain() {
        when(locks.rtVendor(1L)).thenReturn("FINISHED");service.execute(1L);
    }

    /** 累计守恒：本次 4 + 已执行 7 > 不合格 10，执行前拒绝。 */
    @Test public void cumulativeOverUnqualifiedIsRejected() {
        when(locks.rtVendor(1L)).thenReturn("APPROVED"); when(guards.lockMode(1L)).thenReturn("PRE_STOCK");
        when(guards.lockSourceKey(1L)).thenReturn("AUTO:RTV:IQC:2"); when(locks.iqc(2L)).thenReturn("FINISHED");
        when(guards.finishedPreStockReturned(2L, 1L)).thenReturn(new BigDecimal("7"));
        QcIqc iqc=new QcIqc();iqc.setIqcId(2L);iqc.setSourceDocType("ARRIVAL_NOTICE");iqc.setSourceLineId(3L);
        iqc.setItemId(4L);iqc.setQuantityUnqualified(new BigDecimal("10"));when(iqcs.selectQcIqcByIqcId(2L)).thenReturn(iqc);
        WmRtVendor header=new WmRtVendor();header.setRtId(1L);header.setAttr1("AUTO:RTV:IQC:2");header.setAttr2("PRE_STOCK");
        when(headers.selectWmRtVendorByRtId(1L)).thenReturn(header);
        WmRtVendorLine line=new WmRtVendorLine();line.setRtId(1L);line.setItemId(4L);line.setAttr1("IQC:2");line.setAttr2("3");line.setQuantityRted(new BigDecimal("4"));
        when(lines.selectWmRtVendorLineList(any(WmRtVendorLine.class))).thenReturn(Collections.singletonList(line));
        when(details.selectWmRtVendorDetailList(any())).thenReturn(Collections.emptyList());
        try {
            service.execute(1L);
            fail("应拒绝超额退供");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("剩余可退数量"));
        }
        verifyNoInteractions(storage);
        verify(headers, never()).updateWmRtVendor(any());
    }

    /** 未入库退供禁止关联库存拣货明细。 */
    @Test public void preStockRejectsStockDetails() {
        when(locks.rtVendor(1L)).thenReturn("APPROVED"); when(guards.lockMode(1L)).thenReturn("PRE_STOCK");
        when(guards.lockSourceKey(1L)).thenReturn("AUTO:RTV:IQC:2"); when(locks.iqc(2L)).thenReturn("FINISHED");
        when(guards.finishedPreStockReturned(2L, 1L)).thenReturn(BigDecimal.ZERO);
        QcIqc iqc=new QcIqc();iqc.setIqcId(2L);iqc.setSourceDocType("ARRIVAL_NOTICE");iqc.setSourceLineId(3L);
        iqc.setItemId(4L);iqc.setQuantityUnqualified(new BigDecimal("10"));when(iqcs.selectQcIqcByIqcId(2L)).thenReturn(iqc);
        WmRtVendor header=new WmRtVendor();header.setRtId(1L);header.setAttr1("AUTO:RTV:IQC:2");header.setAttr2("PRE_STOCK");
        when(headers.selectWmRtVendorByRtId(1L)).thenReturn(header);
        WmRtVendorLine line=new WmRtVendorLine();line.setRtId(1L);line.setItemId(4L);line.setAttr1("IQC:2");line.setAttr2("3");line.setQuantityRted(new BigDecimal("4"));
        when(lines.selectWmRtVendorLineList(any(WmRtVendorLine.class))).thenReturn(Collections.singletonList(line));
        when(details.selectWmRtVendorDetailList(any())).thenReturn(Collections.singletonList(new com.ktg.mes.wm.domain.WmRtVendorDetail()));
        try {
            service.execute(1L);
            fail("未入库退供不应关联库存明细");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("库存拣货明细"));
        }
        verifyNoInteractions(storage);
    }
}
