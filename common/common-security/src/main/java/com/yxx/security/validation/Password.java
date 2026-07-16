package com.yxx.security.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用系统配置化密码策略校验密码。
 *
 * <p>空值由 {@code @NotBlank} 负责，本注解只负责长度、字节数和复杂度。</p>
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {

    String message() default "密码不符合系统安全策略";

    /** 登录及旧密码校验只限制最大字节数，新密码校验完整策略。 */
    boolean enforcePolicy() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
