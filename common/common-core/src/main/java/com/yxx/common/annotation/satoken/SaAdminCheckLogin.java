package com.yxx.common.annotation.satoken;

import cn.dev33.satoken.annotation.SaCheckLogin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端登录认证：只有管理员登录之后才能进入该方法。
 * <p> 可标注在函数、类上（效果等同于标注在此类的所有方法上）
 */
@SaCheckLogin(type = "admin")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE})
public @interface SaAdminCheckLogin {

}
