package com.ktg.mes;

import com.ktg.mes.cal.domain.CalCalendar;
import com.ktg.mes.cal.domain.CalTeamMember;
import com.ktg.mes.cal.mapper.CalTeamMemberMapper;
import com.ktg.mes.cal.mapper.CalTeamshiftMapper;
import com.ktg.mes.cal.domain.CalTeamshift;
import com.ktg.mes.cal.service.impl.CalCalendarServiceImpl;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CalCalendarUserTest {
    @Test
    public void unassignedAndMissingUserReturnEmptyCalendarWithoutQueryingAnotherTeam() {
        CalTeamMemberMapper mapper = mock(CalTeamMemberMapper.class);
        when(mapper.selectCalTeamMemberList(any(CalTeamMember.class))).thenReturn(Collections.emptyList());
        CalCalendarServiceImpl service = new CalCalendarServiceImpl();
        ReflectionTestUtils.setField(service, "calTeamMemberMapper", mapper);
        Calendar day = Calendar.getInstance(); day.set(2026, Calendar.OCTOBER, 1);
        List<CalCalendar> unassigned = service.getCalendarByUser(day.getTime(), 999L);
        assertEquals(31, unassigned.size());
        assertTrue(unassigned.stream().allMatch(c -> c.getTeamShifts() == null || c.getTeamShifts().isEmpty()));
        List<CalCalendar> missing = service.getCalendarByUser(day.getTime(), null);
        assertEquals(31, missing.size());
        verify(mapper, times(1)).selectCalTeamMemberList(any(CalTeamMember.class));
    }

    @Test
    public void assignedUserQueriesTheirTeam() {
        CalTeamMemberMapper members = mock(CalTeamMemberMapper.class);
        CalTeamshiftMapper shifts = mock(CalTeamshiftMapper.class);
        CalTeamMember member = new CalTeamMember(); member.setTeamId(42L);
        when(members.selectCalTeamMemberList(any(CalTeamMember.class))).thenReturn(Collections.singletonList(member));
        when(shifts.selectCalTeamshiftList(any(CalTeamshift.class))).thenReturn(Collections.emptyList());
        CalCalendarServiceImpl service = new CalCalendarServiceImpl();
        ReflectionTestUtils.setField(service, "calTeamMemberMapper", members);
        ReflectionTestUtils.setField(service, "calTeamshiftMapper", shifts);
        Calendar day = Calendar.getInstance(); day.set(2026, Calendar.OCTOBER, 1);
        assertEquals(31, service.getCalendarByUser(day.getTime(), 8L).size());
        verify(shifts, times(31)).selectCalTeamshiftList(argThat(p -> Long.valueOf(42).equals(p.getTeamId())));
    }
}
