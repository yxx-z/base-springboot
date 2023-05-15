package com.yxx.common.utils.jackson;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
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
 * Jackson工具类
 *
 * @author yxx
 * @since 2022/7/15 16:54
 */
@Slf4j
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class JacksonUtil {

    private final static ObjectMapper OBJECT_MAPPER;

    static {
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
            return (String) object;
        }
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Json转换为对象 转换失败返回null
     *
     * @param json Json字符串
     * @return Object
     */
    public static Object parse(String json) {
        Object object = null;
        try {
            object = getObjectMapper().readValue(json, Object.class);
        } catch (Exception ignored) {
        }
        return object;
    }

    /**
     * Json转换为对象 转换失败返回null
     *
     * @param json  Json字符串
     * @param clazz 转换的类型
     * @return 转换后的对象
     */
    public static <T> T readValue(String json, Class<T> clazz) {
        T t = null;
        try {
            t = getObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    /**
     * Json转换为对象 转换失败返回null
     *
     * @param json         json字符串
     * @param valueTypeRef TypeReference
     * @return 对象
     */
    public static <T> T readValue(String json, TypeReference<T> valueTypeRef) {
        T t = null;
        try {
            t = getObjectMapper().readValue(json, valueTypeRef);
        } catch (Exception e) {
            log.error("JacksonUtil readValue", e);
        }
        return t;
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
        // 忽略不能转移的字符
        objectMapper.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature(), true);
        // 忽略目标对象没有的属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        //忽略transient
        objectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        objectMapper.getSerializerProvider().setNullValueSerializer(new NullValueSerializer());
        return registerModule(objectMapper);
    }

    /**
     * 注册模块
     *
     * @param objectMapper ObjectMapper
     * @return ObjectMapper
     */
    private static ObjectMapper registerModule(ObjectMapper objectMapper) {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Date.class, new DateSerializer(true, null));
        simpleModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)));

        simpleModule.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return DateUtils.convertLocalDateTime(p.getText());
            }
        });
        simpleModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN)));
        simpleModule.addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return DateUtils.convertLocalDate(p.getText());
            }
        });

        simpleModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
        simpleModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }

    /**
     * NULL值处理为空字符串
     */
    public static class NullValueSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object object, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString("");
        }
    }
}
