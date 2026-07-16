package com.yxx.common.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 项目实际使用的日期转换能力。
 *
 * <p>基础工具类只保留有明确调用场景的方法，避免积累大量未经测试的通用日期函数。</p>
 */
public final class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-M-d H:m[:s]");

    private DateUtils() {
    }

    /**
     * 将日期字符串或 10/13 位 Unix 时间戳转换为本地日期。
     */
    public static LocalDate convertLocalDate(String source) {
        String normalized = normalize(source);
        if (normalized == null) {
            return null;
        }
        if (normalized.matches("^\\d{4}-\\d{1,2}$")) {
            return LocalDate.parse(normalized + "-1", DATE_FORMATTER);
        }
        if (normalized.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            return LocalDate.parse(normalized, DATE_FORMATTER);
        }
        Long epochMillis = parseEpochMillis(normalized);
        if (epochMillis != null) {
            return Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
        throw new IllegalArgumentException("无效的日期值：" + normalized);
    }

    /**
     * 将日期时间字符串或 10/13 位 Unix 时间戳转换为本地日期时间。
     */
    public static LocalDateTime convertLocalDateTime(String source) {
        String normalized = normalize(source);
        if (normalized == null) {
            return null;
        }
        if (normalized.matches("^\\d{4}-\\d{1,2}$")) {
            normalized += "-1 0:0:0";
        } else if (normalized.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            normalized += " 0:0:0";
        }
        if (normalized.matches("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}(:\\d{1,2})?$")) {
            return LocalDateTime.parse(normalized, DATETIME_FORMATTER);
        }
        Long epochMillis = parseEpochMillis(normalized);
        if (epochMillis != null) {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        }
        throw new IllegalArgumentException("无效的日期时间值：" + normalized);
    }

    /**
     * 计算当前时刻到下一天零点的剩余秒数，用于自然日频控 Key 的 TTL。
     */
    public static long secondsUntilNextDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDay = now.toLocalDate().plusDays(1).atStartOfDay();
        return Math.max(1L, Duration.between(now, nextDay).getSeconds());
    }

    private static String normalize(String source) {
        if (source == null) {
            return null;
        }
        String normalized = source.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Long parseEpochMillis(String source) {
        if (!source.matches("^\\d+$") || (source.length() != 10 && source.length() != 13)) {
            return null;
        }
        return Long.parseLong(source.length() == 10 ? source + "000" : source);
    }
}
