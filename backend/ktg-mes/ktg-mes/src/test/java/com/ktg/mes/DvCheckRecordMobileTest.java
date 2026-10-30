package com.ktg.mes;

import com.ktg.mes.dv.controller.mobile.DvCheckRecordMobController;
import com.ktg.mes.dv.domain.DvCheckRecord;
import com.ktg.mes.dv.domain.DvCheckRecordLine;
import com.ktg.mes.dv.domain.DvCheckPlan;
import com.ktg.mes.dv.service.IDvCheckPlanService;
import com.ktg.mes.dv.service.IDvCheckRecordLineService;
import com.ktg.mes.dv.service.IDvCheckRecordService;
import com.ktg.mes.dv.service.IDvCheckSubjectService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

public class DvCheckRecordMobileTest {
    private DvCheckRecordMobController controller;
    private IDvCheckPlanService plans;
    private IDvCheckRecordService records;
    private IDvCheckRecordLineService lines;

    @Before public void setup() {
        controller = new DvCheckRecordMobController();
        plans = mock(IDvCheckPlanService.class);
        records = mock(IDvCheckRecordService.class);
        lines = mock(IDvCheckRecordLineService.class);
        ReflectionTestUtils.setField(controller, "dvCheckPlanService", plans);
        ReflectionTestUtils.setField(controller, "dvCheckRecordService", records);
        ReflectionTestUtils.setField(controller, "dvCheckRecordLineService", lines);
        ReflectionTestUtils.setField(controller, "dvCheckSubjectService", mock(IDvCheckSubjectService.class));
    }

    @Test public void missingPlanDoesNotCreateRecord() {
        DvCheckRecord request = new DvCheckRecord();
        request.setMachineryCode("NEW");
        assertNotEquals(200, controller.add(request).get("code"));
        verify(records, never()).insertDvCheckRecord(any());
    }

    @Test public void missingPlanForChangedMachineDoesNotDeleteExistingLines() {
        DvCheckRecord old = new DvCheckRecord(); old.setMachineryCode("OLD");
        when(records.selectDvCheckRecordByRecordId(1L)).thenReturn(old);
        DvCheckRecord request = new DvCheckRecord(); request.setRecordId(1L); request.setMachineryCode("NEW");
        assertNotEquals(200, controller.edit(request).get("code"));
        verify(lines, never()).deleteDvCheckRecordLineByRecordId(any());
        verify(records, never()).updateDvCheckRecord(any());
    }

    @Test public void completingCannotChangeMachine() {
        DvCheckRecord old = new DvCheckRecord(); old.setMachineryCode("OLD"); old.setMachineryId(1L);
        when(records.selectDvCheckRecordByRecordId(1L)).thenReturn(old);
        when(lines.selectDvCheckRecordLineList(any())).thenReturn(Collections.singletonList(new DvCheckRecordLine()));
        DvCheckPlan plan = new DvCheckPlan(); plan.setPlanId(2L);
        when(plans.selectDvCheckPlanByMachineryCodeAndType(anyString(), anyString())).thenReturn(plan);
        DvCheckRecord request = new DvCheckRecord(); request.setRecordId(1L); request.setMachineryCode("NEW"); request.setStatus("FINISHED");
        assertNotEquals(200, controller.edit(request).get("code"));
        verify(lines, never()).deleteDvCheckRecordLineByRecordId(any());
        verify(records, never()).updateDvCheckRecord(any());
    }

    @Test public void completingWithUncheckedLinesIsRejected() {
        DvCheckRecordLine unchecked = new DvCheckRecordLine();
        unchecked.setCheckStatus("O");
        when(lines.selectDvCheckRecordLineList(any())).thenReturn(Collections.singletonList(unchecked));
        DvCheckRecord request = new DvCheckRecord();
        request.setRecordId(1L); request.setStatus("FINISHED");
        assertNotEquals(200, controller.edit(request).get("code"));
        verify(records, never()).updateDvCheckRecord(any());
    }
}
