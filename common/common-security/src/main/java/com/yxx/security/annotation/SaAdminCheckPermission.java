package com.yxx.security.annotation;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.yxx.security.satoken.StpAdminUtil;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端权限认证组合注解。
 */
@Documented
@SaCheckPermission(type = StpAdminUtil.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SaAdminCheckPermission {

    /** 需要校验的权限编码。 */
    @AliasFor(annotation = SaCheckPermission.class)
    String[] value() default {};

    /** 多个权限的组合校验模式。 */
    @AliasFor(annotation = SaCheckPermission.class)
    SaMode mode() default SaMode.AND;
}
