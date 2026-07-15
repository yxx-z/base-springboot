package com.yxx.common.annotation.jackson;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.yxx.common.utils.jackson.QueryDateBoundaryDeserializer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询日期边界转换注解。
 *
 * <p>用于查询请求中的日期字段，将日期转换为当天开始或结束时间。使用枚举避免旧设计中
 * “开始时间”和“结束时间”两个布尔属性同时为真的非法组合。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = QueryDateBoundaryDeserializer.class)
public @interface QueryDateBoundary {

    /** 日期边界类型。 */
    Boundary value();

    /** 支持的日期边界。 */
    enum Boundary {
        /** 当天开始时间。 */
        START_OF_DAY,
        /** 当天结束时间。 */
        END_OF_DAY
    }
}
