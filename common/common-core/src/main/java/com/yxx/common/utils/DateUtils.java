package com.yxx.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * @author yxx
 * @since 2022/7/18 9:40
 */
@SuppressWarnings("unused")
public class DateUtils extends DateUtil {

    private static final String[] PARSE_PATTERNS = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 格式化时间为 yyyy-MM-dd HH:mm:ss
     *
     * @param date 时间
     * @return 转换后字符串
     */
    public static String formatDateTime(Date date) {
        return format(date, DATETIME_FORMAT);
    }

    /**
     * 格式化时间为 yyyy-MM
     * 获取当前年月
     *
     * @return 转换后字符串
     */
    public static String getDateTime(LocalDateTime localDate) {
        //设置日期格式
        return localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }


    /**
     * 格式化时间为 yyyy-MM-dd HH:mm:ss
     *
     * @param dateTime 时间
     * @return 转换后字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return format(dateTime, DATETIME_FORMAT);
    }

    /**
     * Date类型转字符串
     *
     * @param date 日期
     * @return yyyy-MM-dd HH:mm:ss
     */
    public static String date2Str(Date date) {
        return date2Str(null, date);
    }

    /**
     * Date类型转字符串
     *
     * @param format 日期格式
     * @param date   日期
     * @return 转换后的字符串
     */
    public static String date2Str(String format, Date date) {
        if (ObjectUtils.isNull(date)) {
            return null;
        }
        SimpleDateFormat dateFormat = CharSequenceUtil.isBlank(format) ?
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") :
                new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    /**
     * 字符串转date类型
     *
     * @param dateStr 日期字符串
     * @return Date
     */
    public static Date parseDate(Object dateStr) {
        if (ObjectUtils.isNull(dateStr)) {
            return null;
        }
        return parse(dateStr.toString(), PARSE_PATTERNS);
    }

    /**
     * 字符串转LocalTime类型
     *
     * @param dateStr 日期字符串
     * @return LocalTime
     */
    public static LocalTime parseLocalTime(Object dateStr) {
        if (ObjectUtils.isNull(dateStr)) {
            return null;
        }
        if (dateStr instanceof Double) {
            return date2LocalTime(parseDate(dateStr));
        }
        return LocalTime.parse(dateStr.toString(), TIME_FORMATTER);
    }

    /**
     * Date转LocalTime
     *
     * @param date 日期
     * @return LocalTime
     */
    public static LocalTime date2LocalTime(Date date) {
        if (null == date) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
    }

    /**
     * LocalTime转Str
     *
     * @param localTime 时间
     * @return HH:mm:ss
     */
    public static String localTime2Str(LocalTime localTime) {
        return localTime2Str(null, localTime);
    }

    /**
     * LocalTime转str
     *
     * @param format    格式
     * @param localTime 时间
     * @return 转换后的时间字符串
     */
    public static String localTime2Str(String format, LocalTime localTime) {
        if (null == localTime) {
            return null;
        }
        DateTimeFormatter formatter = CharSequenceUtil.isBlank(format) ?
                TIME_FORMATTER : DateTimeFormatter.ofPattern(format);
        return localTime.format(formatter);
    }

    /**
     * 字符串转LocalDate类型
     *
     * @param dateStr 日期字符串
     * @return LocalDate
     */
    public static LocalDate parseLocalDate(Object dateStr) {
        if (ObjectUtils.isNull(dateStr)) {
            return null;
        }
        if (dateStr instanceof Double) {
            return date2LocalDate(parseDate(dateStr));
        }
        return LocalDate.parse(dateStr.toString(), DATE_FORMATTER);
    }

    /**
     * Date转LocalDate
     *
     * @param date 日期
     * @return LocalDate
     */
    public static LocalDate date2LocalDate(Date date) {
        if (null == date) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * LocalDate转Str
     *
     * @param localDate 日期
     * @return 转换后的字符串
     */
    public static String localDate2Str(LocalDate localDate) {
        return localDate2Str(null, localDate);
    }

    /**
     * LocalDate转Str
     *
     * @param format    格式
     * @param localDate 日期
     * @return 转换后的字符串
     */
    public static String localDate2Str(String format, LocalDate localDate) {
        if (null == localDate) {
            return null;
        }
        DateTimeFormatter formatter = CharSequenceUtil.isBlank(format) ?
                DATE_FORMATTER : DateTimeFormatter.ofPattern(format);
        return localDate.format(formatter);
    }

    /**
     * 字符串转LocalDateTime类型
     *
     * @param dateStr 日期字符串
     * @return LocalDateTime
     */
    public static LocalDateTime parseLocalDateTime(Object dateStr) {
        if (ObjectUtils.isNull(dateStr)) {
            return null;
        }
        if (dateStr instanceof Double) {
            return date2LocalDateTime(parseDate(dateStr));
        }
        return LocalDateTime.parse(dateStr.toString(), DATETIME_FORMATTER);
    }

    /**
     * Date转LocalDateTime
     *
     * @param date 日期
     * @return LocalDateTime
     */
    public static LocalDateTime date2LocalDateTime(Date date) {
        if (null == date) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * LocalDate转Str
     *
     * @param localDateTime 时间
     * @return 转换后的日期字符串
     */
    public static String localDateTime2Str(LocalDateTime localDateTime) {
        return localDateTime2Str(null, localDateTime);
    }

    /**
     * LocalDate转Str
     *
     * @param format        格式
     * @param localDateTime 时间
     * @return 转换后的日期字符串
     */
    public static String localDateTime2Str(String format, LocalDateTime localDateTime) {
        if (null == localDateTime) {
            return null;
        }
        DateTimeFormatter formatter = CharSequenceUtil.isBlank(format) ?
                DATETIME_FORMATTER : DateTimeFormatter.ofPattern(format);
        return localDateTime.format(formatter);
    }

    public static LocalDate convertLocalDate(String source) {
        source = source.trim();
        if ("".equals(source)) {
            return null;
        }
        if (source.matches("^\\d{4}-\\d{1,2}$")) {
            // yyyy-MM
            return LocalDate.parse(source + "-01", DATE_FORMATTER);
        } else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            // yyyy-MM-dd
            return LocalDate.parse(source, DATE_FORMATTER);
        } else if (NumberUtil.isLong(source)) {
            if (source.length() == 10 || source.length() == 13) {
                source = source.length() == 10 ? source + "000" : source;
                return Instant.ofEpochMilli(Long.parseLong(source)).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            return null;
        } else {
            throw new IllegalArgumentException("Invalid datetime value '" + source + "'");
        }
    }

    public static LocalDateTime convertLocalDateTime(String source) {
        source = source.trim();
        if ("".equals(source)) {
            return null;
        }
        if (source.matches("^\\d{4}-\\d{1,2}$")) {
            // yyyy-MM
            return LocalDateTime.parse(source + "-01 00:00:00", DATETIME_FORMATTER);
        } else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            // yyyy-MM-dd
            return LocalDateTime.parse(source + " 00:00:00", DATETIME_FORMATTER);
        } else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}$")) {
            // yyyy-MM-dd HH:mm
            return LocalDateTime.parse(source + ":00", DATETIME_FORMATTER);
        } else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$")) {
            // yyyy-MM-dd HH:mm:ss
            return LocalDateTime.parse(source, DATETIME_FORMATTER);
        } else if (NumberUtil.isLong(source)) {
            if (source.length() == 10 || source.length() == 13) {
                source = source.length() == 10 ? source + "000" : source;
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(source)), ZoneId.systemDefault());
            }
            return null;
        } else {
            throw new IllegalArgumentException("Invalid datetime value '" + source + "'");
        }
    }

    /**
     * 传入 字符串格式 日期，获得周几
     *
     * @param date 字符串类型日期
     * @return 周几： 1-周一；2-周二；3-周三；4-周四；5-周五；6-周六；7-周日
     */
    public static Integer whichDayToWeekByStr(String date) {
        try {
            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date enteringDate = sdf.parse(date);

            // 创建Calendar类实例
            Calendar instance = Calendar.getInstance();
            // 获取传入的日期是周几
            instance.setTime(enteringDate);
            int i = instance.get(Calendar.DAY_OF_WEEK);
            // 获取周日
            int sunday = Calendar.SUNDAY;
            // 如果是周日 特殊处理
            if (i == sunday) {
                return 7;
            } else {
                return i - 1;
            }
        } catch (ParseException e) {
            throw new ApiException(ApiCode.DATE_ERROR);
        }
    }

    /**
     * 传入 LocalDateTime 格式日期 获得周几
     *
     * @param enteringDate LocalDateTime格式日期
     * @return 周几： 1-周一；2-周二；3-周三；4-周四；5-周五；6-周六；7-周日
     */
    public static Integer whichDayToWeekByLDT(LocalDate enteringDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = enteringDate.atStartOfDay().atZone(zoneId);
        Date date = Date.from(zdt.toInstant());

        // 创建Calendar类实例
        Calendar instance = Calendar.getInstance();
        // 获取传入的日期是周几
        instance.setTime(date);
        int i = instance.get(Calendar.DAY_OF_WEEK);
        // 获取周日
        int sunday = Calendar.SUNDAY;
        // 如果是周日 特殊处理
        if (i == sunday) {
            return 7;
        } else {
            return i - 1;
        }
    }

    /**
     * 传入 LocalDateTime 格式日期 和 n 天 获取n天后的日期
     *
     * @param enteringDate 开始时间 LocalDateTime格式
     * @param day          n为正数 为后n天； n为负数 为前n天
     * @return n天后的日期 LocalDateTime 格式
     */
    public static LocalDate getBeforeNumDay(LocalDate enteringDate, int day) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = enteringDate.atStartOfDay().atZone(zoneId);
        Date date = Date.from(zdt.toInstant());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, day);
        Date time = calendar.getTime();

        Instant instant = time.toInstant();
        return instant.atZone(zoneId).toLocalDate();
    }

    /**
     * 今天剩余时间 单位(秒)
     *
     * @return {@link Long }
     * @author yxx
     */
    public static Long theRestOfTheDaySecond(){
        // 获取当前日期和时间
        LocalDateTime now = LocalDateTime.now();

        // 获取今晚的十二点整时间
        LocalDateTime midnight = now.toLocalDate().atTime(LocalTime.MAX);

        // 计算当前时间距离今晚十二点整的秒数
        Duration duration = Duration.between(now, midnight);
        return duration.getSeconds();
    }
}
