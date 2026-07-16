package com.yxx.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按去除首尾空白后的真实长度校验文本。
 *
 * <p>普通 {@code @Size} 在字段规范化之前执行，类似“空格+a+空格”的输入可能用空白字符
 * 凑够最小长度。本约束与 {@code AccountNormalizer} 的 trim 语义保持一致。</p>
 */
@Documented
@Constraint(validatedBy = TrimmedSizeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface TrimmedSize {

    int min() default 0;

    int max() default Integer.MAX_VALUE;

    String message() default "文本规范化后的长度不合法";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
