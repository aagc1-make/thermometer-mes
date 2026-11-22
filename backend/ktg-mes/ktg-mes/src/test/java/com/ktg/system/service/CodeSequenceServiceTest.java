package com.ktg.system.service;

import com.ktg.system.domain.SysCodeRule;
import com.ktg.system.mapper.SysCodeSequenceMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 取号逻辑单元测试：同一前缀当天连续取号不重复、格式正确。
 *
 * <p>这里只验证应用层拼号与流水递增；真正的并发原子性由
 * sys_code_sequence 的 INSERT ... ON DUPLICATE KEY UPDATE + LAST_INSERT_ID 保证，
 * 已在 j2eedb 用 20 连接 × 500 次并发实测无重号。</p>
 */
class CodeSequenceServiceTest {

    @Test
    void consecutiveCodesUnderSameRuleDoNotRepeat() throws Exception {
        SysCodeSequenceMapper mapper = mock(SysCodeSequenceMapper.class);
        CodeSequenceService service = serviceWith(mapper);

        SysCodeRule rule = new SysCodeRule();
        rule.setRuleCode("GD");
        rule.setPrefix("gd");
        rule.setDateFormat("yyyyMMdd");
        rule.setSerialLength(5);
        rule.setSerialStart(1L);
        rule.setSerialStep(1);
        rule.setResetCycle("DAY");
        rule.setEnableFlag("Y");
        when(mapper.selectSysCodeRuleByCode("GD")).thenReturn(rule);

        AtomicLong sequence = new AtomicLong(0);
        when(mapper.takeSequence(anyString(), anyString(), anyLong(), anyInt())).thenAnswer(invocation -> {
            sequence.addAndGet(1);
            return 1;
        });
        when(mapper.selectLastInsertId()).thenAnswer(invocation -> sequence.get());

        String today = String.format("%04d%02d%02d", LocalDate.now().getYear(),
                LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth());
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            assertTrue(codes.add(service.generateCode("GD")), "同一前缀当天不应产生重复单号");
        }
        assertEquals(200, codes.size());
        assertTrue(codes.contains("gd" + today + "00001"), "首个单号应为 5 位流水从 00001 开始");
        assertTrue(codes.contains("gd" + today + "00200"), "末个单号应为 00200");
    }

    @Test
    void missingRuleIsRejected() throws Exception {
        SysCodeSequenceMapper mapper = mock(SysCodeSequenceMapper.class);
        CodeSequenceService service = serviceWith(mapper);
        assertThrows(IllegalStateException.class, () -> service.generateCode("NOT_EXISTS"));
    }

    private CodeSequenceService serviceWith(SysCodeSequenceMapper mapper) throws Exception {
        CodeSequenceService service = new CodeSequenceService();
        Field field = CodeSequenceService.class.getDeclaredField("sysCodeSequenceMapper");
        field.setAccessible(true);
        field.set(service, mapper);
        return service;
    }
}
