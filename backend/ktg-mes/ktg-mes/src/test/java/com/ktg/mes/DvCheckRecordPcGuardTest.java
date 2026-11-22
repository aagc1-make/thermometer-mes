package com.ktg.mes;

import com.ktg.mes.dv.controller.DvCheckRecordController;
import com.ktg.mes.dv.domain.DvCheckRecord;
import com.ktg.mes.dv.domain.DvCheckPlan;
import com.ktg.mes.dv.domain.DvCheckMachinery;
import com.ktg.mes.dv.domain.DvCheckRecordLine;
import com.ktg.mes.dv.service.IDvCheckRecordService;
import com.ktg.mes.dv.service.IDvCheckPlanService;
import com.ktg.mes.dv.service.IDvCheckMachineryService;
import com.ktg.mes.dv.service.IDvCheckSubjectService;
import com.ktg.mes.dv.service.IDvCheckRecordLineService;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

public class DvCheckRecordPcGuardTest {
    @Test public void missingPlanAndEquipmentCannotCreateRecord() {
        DvCheckRecordController controller = new DvCheckRecordController();
        IDvCheckRecordService records = mock(IDvCheckRecordService.class);
        ReflectionTestUtils.setField(controller, "dvCheckRecordService", records);
        assertNotEquals(200, controller.add(new DvCheckRecord()).get("code"));
        verify(records, never()).insertDvCheckRecord(any());
    }

    @Test public void planStatusTypeAndEquipmentAreCheckedBeforeCreate() {
        DvCheckRecordController controller = new DvCheckRecordController();
        IDvCheckRecordService records = mock(IDvCheckRecordService.class);
        IDvCheckPlanService plans = mock(IDvCheckPlanService.class);
        IDvCheckMachineryService machines = mock(IDvCheckMachineryService.class);
        IDvCheckSubjectService subjects = mock(IDvCheckSubjectService.class);
        ReflectionTestUtils.setField(controller, "dvCheckRecordService", records);
        ReflectionTestUtils.setField(controller, "dvCheckPlanService", plans);
        ReflectionTestUtils.setField(controller, "dvCheckMachineryService", machines);
        ReflectionTestUtils.setField(controller, "dvCheckSubjectService", subjects);
        DvCheckRecord request = new DvCheckRecord(); request.setPlanId(1L);
        request.setMachineryId(2L); request.setMachineryCode("M2");
        DvCheckPlan plan = new DvCheckPlan(); plan.setPlanType("CHECK"); plan.setStatus("PREPARE");
        when(plans.selectDvCheckPlanByPlanId(1L)).thenReturn(plan);
        assertNotEquals(200, controller.add(request).get("code"));
        plan.setStatus("CANCELED"); assertNotEquals(200, controller.add(request).get("code"));
        plan.setStatus("FINISHED"); plan.setPlanType("MAINTEN");
        assertNotEquals(200, controller.add(request).get("code"));
        plan.setPlanType("CHECK"); assertNotEquals(200, controller.add(request).get("code"));
        verify(records, never()).insertDvCheckRecord(any());
        when(machines.selectDvCheckMachineryList(any())).thenReturn(Collections.singletonList(new DvCheckMachinery()));
        assertEquals(200, controller.add(request).get("code"));
        plan.setStatus("CONFIRMED"); assertEquals(200, controller.add(request).get("code"));
        verify(records, times(2)).insertDvCheckRecord(request);
    }

    @Test public void snapshotFieldsCannotBeChangedWithoutChangingPlan() {
        DvCheckRecordController controller = new DvCheckRecordController();
        IDvCheckRecordService records = mock(IDvCheckRecordService.class);
        ReflectionTestUtils.setField(controller, "dvCheckRecordService", records);
        DvCheckRecord old = new DvCheckRecord(); old.setPlanCode("REAL"); old.setPlanName("Real plan"); old.setPlanType("CHECK");
        when(records.selectDvCheckRecordByRecordId(1L)).thenReturn(old);
        when(records.updateDvCheckRecord(any())).thenReturn(1);
        DvCheckRecord request = new DvCheckRecord(); request.setRecordId(1L); request.setPlanCode("FORGED"); request.setPlanName("FORGED");
        assertEquals(200, controller.edit(request).get("code"));
        assertEquals("REAL", request.getPlanCode()); assertEquals("Real plan", request.getPlanName());
    }

    @Test public void completingCannotChangePlan() {
        DvCheckRecordController controller = new DvCheckRecordController();
        IDvCheckRecordService records = mock(IDvCheckRecordService.class);
        IDvCheckRecordLineService lines = mock(IDvCheckRecordLineService.class);
        IDvCheckPlanService plans = mock(IDvCheckPlanService.class);
        IDvCheckMachineryService machines = mock(IDvCheckMachineryService.class);
        IDvCheckSubjectService subjects = mock(IDvCheckSubjectService.class);
        ReflectionTestUtils.setField(controller, "dvCheckRecordService", records);
        ReflectionTestUtils.setField(controller, "dvCheckRecordLineService", lines);
        ReflectionTestUtils.setField(controller, "dvCheckPlanService", plans);
        ReflectionTestUtils.setField(controller, "dvCheckMachineryService", machines);
        ReflectionTestUtils.setField(controller, "dvCheckSubjectService", subjects);
        DvCheckRecord old = new DvCheckRecord(); old.setPlanId(1L); old.setMachineryId(2L); old.setMachineryCode("M2");
        when(records.selectDvCheckRecordByRecordId(3L)).thenReturn(old);
        when(lines.selectDvCheckRecordLineList(any())).thenReturn(Collections.singletonList(new DvCheckRecordLine()));
        DvCheckPlan plan = new DvCheckPlan(); plan.setPlanType("CHECK"); plan.setStatus("FINISHED");
        when(plans.selectDvCheckPlanByPlanId(4L)).thenReturn(plan);
        when(machines.selectDvCheckMachineryList(any())).thenReturn(Collections.singletonList(new DvCheckMachinery()));
        when(records.updateDvCheckRecord(any())).thenReturn(1);
        DvCheckRecord request = new DvCheckRecord(); request.setRecordId(3L); request.setPlanId(4L); request.setStatus("FINISHED");
        assertNotEquals(200, controller.edit(request).get("code"));
        verify(records, never()).updateDvCheckRecord(any());
        verify(lines, never()).deleteDvCheckRecordLineByRecordId(any());
    }

    @Test public void completingWithUncheckedLinesIsRejected() {
        DvCheckRecordController controller = new DvCheckRecordController();
        IDvCheckRecordService records = mock(IDvCheckRecordService.class);
        IDvCheckRecordLineService lines = mock(IDvCheckRecordLineService.class);
        ReflectionTestUtils.setField(controller, "dvCheckRecordService", records);
        ReflectionTestUtils.setField(controller, "dvCheckRecordLineService", lines);
        DvCheckRecordLine unchecked = new DvCheckRecordLine();
        unchecked.setCheckStatus("O");
        when(lines.selectDvCheckRecordLineList(any())).thenReturn(Collections.singletonList(unchecked));
        DvCheckRecord request = new DvCheckRecord();
        request.setRecordId(3L); request.setStatus("FINISHED");
        assertNotEquals(200, controller.edit(request).get("code"));
        verify(records, never()).updateDvCheckRecord(any());
    }

    @Test public void completingWithCheckedLinesPasses() {
        DvCheckRecordController controller = new DvCheckRecordController();
        IDvCheckRecordService records = mock(IDvCheckRecordService.class);
        IDvCheckRecordLineService lines = mock(IDvCheckRecordLineService.class);
        ReflectionTestUtils.setField(controller, "dvCheckRecordService", records);
        ReflectionTestUtils.setField(controller, "dvCheckRecordLineService", lines);
        DvCheckRecordLine checked = new DvCheckRecordLine();
        checked.setCheckStatus("Y");
        when(lines.selectDvCheckRecordLineList(any())).thenReturn(Collections.singletonList(checked));
        DvCheckRecord old = new DvCheckRecord();
        old.setPlanCode("TG-CP-01"); old.setPlanName("plan"); old.setPlanType("CHECK");
        when(records.selectDvCheckRecordByRecordId(3L)).thenReturn(old);
        when(records.updateDvCheckRecord(any())).thenReturn(1);
        DvCheckRecord request = new DvCheckRecord();
        request.setRecordId(3L); request.setStatus("FINISHED");
        assertEquals(200, controller.edit(request).get("code"));
        verify(records, times(1)).updateDvCheckRecord(any());
    }
}
