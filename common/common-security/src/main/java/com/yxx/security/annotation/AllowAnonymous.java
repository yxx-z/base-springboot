package com.yxx.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 允许匿名访问。
 *
 * <p>标注在 Controller 类或方法上后，请求无需携带登录凭证。该名称直接表达访问控制语义，
 * 不再使用容易被误解为“释放 Token”的命名。</p>
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowAnonymous {
}
