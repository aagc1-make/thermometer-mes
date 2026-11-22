package com.ktg.mes;

import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.VendorReturnGuardMapper;
import com.ktg.mes.wm.service.*;
import com.ktg.mes.wm.service.impl.StockVendorReturnDraftService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StockVendorReturnDraftServiceTest {
    @Test public void existingDraftIsReturnedWithoutCreatingAnother() {
        PostingLockMapper locks=mock(PostingLockMapper.class); VendorReturnGuardMapper guards=mock(VendorReturnGuardMapper.class);
        IWmRtVendorService vendors=mock(IWmRtVendorService.class); IWmRtVendorLineService vendorLines=mock(IWmRtVendorLineService.class);
        IWmItemRecptService receipts=mock(IWmItemRecptService.class); IWmItemRecptLineService receiptLines=mock(IWmItemRecptLineService.class);
        IWmItemRecptDetailService details=mock(IWmItemRecptDetailService.class); AutoCodeUtil codes=mock(AutoCodeUtil.class);
        StockVendorReturnDraftService service=new StockVendorReturnDraftService(locks,guards,vendors,vendorLines,receipts,receiptLines,details,codes);
        when(locks.itemRecpt(1L)).thenReturn("FINISHED"); when(guards.openStockDraft(1L)).thenReturn(2L);
        assertEquals(Long.valueOf(2L),service.create(1L,"operator"));
        verifyNoInteractions(vendors,vendorLines);
    }
}
