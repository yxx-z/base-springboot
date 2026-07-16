package com.yxx.common.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 日期转换工具测试。 */
class DateUtilsTest {

    @Test
    void shouldParseSupportedDateFormats() {
        assertEquals(LocalDate.of(2026, 7, 1), DateUtils.convertLocalDate("2026-7-1"));
        assertEquals(LocalDateTime.of(2026, 7, 1, 8, 9),
                DateUtils.convertLocalDateTime("2026-7-1 8:9"));
    }

    @Test
    void shouldReturnPositiveNaturalDayTtl() {
        long seconds = DateUtils.secondsUntilNextDay();
        assertTrue(seconds > 0 && seconds <= 24 * 60 * 60);
    }
}
