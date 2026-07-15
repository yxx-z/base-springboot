package com.yxx.common.annotation.jackson;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.yxx.common.utils.jackson.SearchDateDeserializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author yxx
 * @since 2022/12/2 10:36
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = SearchDateDeserializer.class)
public @interface SearchDate {

    /**
     * 开始时间
     */
    boolean startDate() default false;

    /**
     * 结束时间
     */
    boolean endDate() default false;
}
