package com.yxx.security.annotation;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.yxx.security.satoken.StpAdminUtil;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端角色认证组合注解。
 */
@Documented
@SaCheckRole(type = StpAdminUtil.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SaAdminCheckRole {

    /** 需要校验的角色编码。 */
    @AliasFor(annotation = SaCheckRole.class)
    String[] value() default {};

    /** 多个角色的组合校验模式。 */
    @AliasFor(annotation = SaCheckRole.class)
    SaMode mode() default SaMode.AND;
}
