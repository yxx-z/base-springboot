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
        // 所有入口先统一处理 null、空串和首尾空白，避免后续正则分支重复判断。
        String normalized = normalize(source);
        if (normalized == null) {
            return null;
        }
        // 只提供到月份时，按该月第一天解释，适用于按月筛选条件的起始边界。
        if (normalized.matches("^\\d{4}-\\d{1,2}$")) {
            return LocalDate.parse(normalized + "-1", DATE_FORMATTER);
        }
        // 完整日期直接按允许一位月份、日期的格式解析。
        if (normalized.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            return LocalDate.parse(normalized, DATE_FORMATTER);
        }
        // 数字输入仅接受标准 10 位秒级或 13 位毫秒级时间戳，拒绝含糊的其他长度。
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
        // 日期时间入口沿用相同的空值规范，空白参数不应被解释为当前时间。
        String normalized = normalize(source);
        if (normalized == null) {
            return null;
        }
        // 缺失的日期或时间部分统一补为对应周期的起点，保证查询边界语义稳定。
        if (normalized.matches("^\\d{4}-\\d{1,2}$")) {
            normalized += "-1 0:0:0";
        } else if (normalized.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            normalized += " 0:0:0";
        }
        // 文本格式匹配成功后再交给 DateTimeFormatter，避免解析器接受非约定格式。
        if (normalized.matches("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}(:\\d{1,2})?$")) {
            return LocalDateTime.parse(normalized, DATETIME_FORMATTER);
        }
        // 时间戳按 JVM 默认时区转换；部署环境必须显式配置统一时区以避免跨环境差异。
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
        // 频控窗口按服务器自然日计算，而不是固定追加 24 小时。
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDay = now.toLocalDate().plusDays(1).atStartOfDay();
        // 极端时钟跳变场景至少保留 1 秒 TTL，禁止产生无过期时间的计数 Key。
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
        // 先限制为纯数字和明确长度，防止 Long.parseLong 接受带符号值或产生单位歧义。
        if (!source.matches("^\\d+$") || (source.length() != 10 && source.length() != 13)) {
            return null;
        }
        // 秒级时间戳补齐三位毫秒后，与毫秒级时间戳统一进入 Instant 转换流程。
        return Long.parseLong(source.length() == 10 ? source + "000" : source);
    }
}
