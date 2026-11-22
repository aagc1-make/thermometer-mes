package com.ktg.mes;

import com.ktg.mes.wm.domain.WmRtVendorDetail;
import com.ktg.mes.wm.domain.WmRtVendorLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.VendorReturnGuardMapper;
import com.ktg.mes.wm.mapper.WmRtVendorDetailMapper;
import com.ktg.mes.wm.mapper.WmRtVendorLineMapper;
import com.ktg.mes.wm.service.impl.WmRtVendorLineServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class VendorReturnDraftDeletionTest {
    private WmRtVendorLineServiceImpl service;
    private WmRtVendorLineMapper lines;
    private WmRtVendorDetailMapper details;
    @Before public void setup() {
        service = new WmRtVendorLineServiceImpl();
        lines = mock(WmRtVendorLineMapper.class);
        details = mock(WmRtVendorDetailMapper.class);
        PostingLockMapper locks = mock(PostingLockMapper.class);
        VendorReturnGuardMapper guards = mock(VendorReturnGuardMapper.class);
        ReflectionTestUtils.setField(service, "wmRtVendorLineMapper", lines);
        ReflectionTestUtils.setField(service, "wmRtVendorDetailMapper", details);
        ReflectionTestUtils.setField(service, "locks", locks);
        ReflectionTestUtils.setField(service, "guards", guards);
        when(locks.rtVendor(1L)).thenReturn("PREPARE");
        when(locks.rtVendorIdForLine(2L)).thenReturn(1L);
        WmRtVendorLine line = new WmRtVendorLine(); line.setLineId(2L); line.setRtId(1L);
        when(lines.selectWmRtVendorLineByLineId(2L)).thenReturn(line);
        WmRtVendorDetail detail = new WmRtVendorDetail(); detail.setDetailId(3L);
        when(details.selectWmRtVendorDetailList(any(WmRtVendorDetail.class))).thenReturn(Collections.singletonList(detail));
    }
    @Test public void deletingDraftLineAlsoDeletesItsPickingDetails() {
        service.deleteWmRtVendorLineByLineId(2L);
        verify(details).deleteWmRtVendorDetailByDetailIds(new Long[]{3L});
        verify(lines).deleteWmRtVendorLineByLineId(2L);
    }
    @Test public void deletingDraftHeaderAlsoDeletesItsPickingDetails() {
        service.deleteByRtId(1L);
        verify(details).deleteWmRtVendorDetailByDetailIds(new Long[]{3L});
        verify(lines).deleteByRtId(1L);
    }
}
