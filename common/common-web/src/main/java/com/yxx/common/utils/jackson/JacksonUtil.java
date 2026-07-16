package com.yxx.common.utils.jackson;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.yxx.common.utils.DateUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

/**
 * Jackson JSON 工具类。
 *
 * @author yxx
 * @since 2022/7/15 16:54
 */
@Slf4j
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class JacksonUtil {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        // 静态工具复用同一线程安全 ObjectMapper，避免每次转换重复构建模块和缓存。
        OBJECT_MAPPER = initObjectMapper(new ObjectMapper());
    }

    /**
     * 转换Json
     *
     * @param object 需要转换的对象
     * @return String
     */
    public static String toJson(Object object) {
        if (Boolean.TRUE.equals(isCharSequence(object))) {
            // 字符串本身已是文本，避免再次序列化后增加一层引号与转义。
            return (String) object;
        }
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            // 转换失败属于调用方数据或模型问题，携带原始异常交给统一异常链处理。
            throw new RuntimeException(e);
        }
    }

    /**
     * JSON 转换为通用对象。
     *
     * @param json Json字符串
     * @return Object
     */
    public static Object parse(String json) {
        try {
            return getObjectMapper().readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON 转换对象失败", exception);
        }
    }

    /**
     * JSON 转换为指定类型对象。
     *
     * @param json  Json字符串
     * @param clazz 转换的类型
     * @return 转换后的对象
     */
    public static <T> T readValue(String json, Class<T> clazz) {
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON 转换对象失败，targetType=" + clazz.getName(), exception);
        }
    }

    /**
     * JSON 转换为泛型对象。
     *
     * @param json         json字符串
     * @param valueTypeRef TypeReference
     * @return 对象
     */
    public static <T> T readValue(String json, TypeReference<T> valueTypeRef) {
        try {
            return getObjectMapper().readValue(json, valueTypeRef);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON 转换泛型对象失败", exception);
        }
    }

    /**
     * 获取ObjectMapper
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * <p>
     * 是否为CharSequence类型
     * </p>
     *
     * @param object 对象
     * @return boolean
     */
    public static Boolean isCharSequence(Object object) {
        return !Objects.isNull(object) && CharSequence.class.isAssignableFrom(object.getClass());
    }

    /**
     * 初始化 ObjectMapper
     *
     * @param objectMapper ObjectMapper
     * @return ObjectMapper
     */
    public static ObjectMapper initObjectMapper(ObjectMapper objectMapper) {
        if (Objects.isNull(objectMapper)) {
            // 允许独立工具调用传 null；Spring 配置通常传入容器管理的实例进行统一初始化。
            objectMapper = new ObjectMapper();
        }
        return doInitObjectMapper(objectMapper);
    }

    /**
     * 初始化 ObjectMapper 时间方法
     *
     * @param objectMapper ObjectMapper
     * @return ObjectMapper
     */
    private static ObjectMapper doInitObjectMapper(ObjectMapper objectMapper) {
        // 所有配置必须直接作用于传入实例，禁止创建后丢弃未使用的 JsonMapper.Builder。
        objectMapper.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature(), true);
        // 基础框架对新增响应字段保持向前兼容，反序列化时忽略调用方暂不认识的属性。
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 时间统一输出可读字符串，禁止使用难以跨语言确认时区的时间戳。
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        return registerModule(objectMapper);
    }

    /**
     * 注册模块
     *
     * @param objectMapper ObjectMapper
     * @return ObjectMapper
     */
    private static ObjectMapper registerModule(ObjectMapper objectMapper) {
        // 传统 Date 与 Java Time API 统一采用项目约定的日期时间格式。
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Date.class, new DateSerializer(true, null));
        simpleModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)));

        simpleModule.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                // 复用 DateUtils 的兼容解析规则，避免 Web 与工具类产生两套日期语义。
                return DateUtils.convertLocalDateTime(p.getText());
            }
        });
        simpleModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN)));
        simpleModule.addDeserializer(LocalDate.class, new JsonDeserializer<>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return DateUtils.convertLocalDate(p.getText());
            }
        });

        simpleModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
        simpleModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
        // 模块最终注册到传入实例，调用方可继续在同一 ObjectMapper 上追加项目扩展模块。
        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }

}
