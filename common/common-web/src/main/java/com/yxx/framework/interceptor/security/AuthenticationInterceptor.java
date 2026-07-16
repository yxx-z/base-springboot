package com.yxx.framework.interceptor.security;

import cn.dev33.satoken.stp.StpUtil;
import com.yxx.security.annotation.AllowAnonymous;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.context.SlidingSessionRenewalService;
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

    private final SlidingSessionRenewalService slidingSessionRenewalService;

    public AuthenticationInterceptor(SlidingSessionRenewalService slidingSessionRenewalService) {
        this.slidingSessionRenewalService = slidingSessionRenewalService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检请求不会携带业务 Token，应直接交给跨域处理链。
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // 静态资源等非 Controller 处理器不参与方法注解认证判断。
            return true;
        }

        Class<?> controllerType = handlerMethod.getBeanType();
        Method controllerMethod = handlerMethod.getMethod();
        // 类级注解适合整个公开 Controller，方法级注解用于最小范围开放单个接口。
        boolean anonymous = controllerType.isAnnotationPresent(AllowAnonymous.class)
                || controllerMethod.isAnnotationPresent(AllowAnonymous.class);
        if (anonymous) {
            return true;
        }

        if (SecurityRealm.ADMIN.equals(applicationRealm)) {
            // 管理端必须检查独立账号体系，不能接受普通用户 Token。
            StpAdminUtil.checkLogin();
        } else if (SecurityRealm.USER.equals(applicationRealm)) {
            // 用户端只接受默认 StpLogic 创建的普通用户 Token。
            StpUtil.checkLogin();
        } else {
            // 未知安全域属于部署配置错误，必须拒绝请求而不是默认当作普通用户端。
            throw new IllegalStateException("不支持的应用安全域：" + applicationRealm);
        }
        // 登录校验成功后再续签，实现“最后一次有效访问后七天过期”。
        slidingSessionRenewalService.renewCurrentSession(applicationRealm);
        return true;
    }
}
