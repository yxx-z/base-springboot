package com.yxx.security.annotation;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.yxx.security.satoken.StpAdminUtil;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端登录认证组合注解。
 */
@Documented
@SaCheckLogin(type = StpAdminUtil.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SaAdminCheckLogin {
}
