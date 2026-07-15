package com.yxx.framework.interceptor.security;

import cn.dev33.satoken.stp.StpUtil;
import com.yxx.security.annotation.AllowAnonymous;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.satoken.StpAdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * Web 请求登录认证拦截器。
 *
 * <p>接口默认要求登录，只有显式声明 {@link AllowAnonymous} 的类或方法允许匿名访问。
 * 响应包装与认证检查分别由不同拦截器负责，避免一个组件同时承担无关职责。</p>
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Value("${app.name}")
    private String applicationRealm;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检请求不会携带业务 Token，应直接交给跨域处理链。
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Class<?> controllerType = handlerMethod.getBeanType();
        Method controllerMethod = handlerMethod.getMethod();
        boolean anonymous = controllerType.isAnnotationPresent(AllowAnonymous.class)
                || controllerMethod.isAnnotationPresent(AllowAnonymous.class);
        if (anonymous) {
            return true;
        }

        if (SecurityRealm.ADMIN.equals(applicationRealm)) {
            StpAdminUtil.checkLogin();
        } else {
            StpUtil.checkLogin();
        }
        return true;
    }
}
