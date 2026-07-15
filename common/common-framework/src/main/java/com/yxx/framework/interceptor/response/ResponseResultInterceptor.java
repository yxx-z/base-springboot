package com.yxx.framework.interceptor.response;

import cn.dev33.satoken.stp.StpUtil;
import com.yxx.common.annotation.auth.ReleaseToken;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.utils.satoken.StpAdminUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;

import java.lang.reflect.Method;

/**
 * @author yxx
 * @since 2022/11/12 03:21
 */
@Component
public class ResponseResultInterceptor implements HandlerInterceptor {
    /**
     * 标记名称
     */
    public static final String RESPONSE_RESULT_ANN = "RESPONSE-RESULT";

    @Value("${app.name}")
    private String appName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 浏览器的 CORS 预检请求不携带业务 Token，应交由跨域配置直接处理。
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (handler instanceof HandlerMethod handlerMethod) {
            final Class<?> clazz = handlerMethod.getBeanType();
            final Method method = handlerMethod.getMethod();

            // 判断是否在类对象上添加了格式化返回结果注解
            if (clazz.isAnnotationPresent(ResponseResult.class)) {
                // 设置此请求返回体，需要包装，往下传递，在ResponseBodyAdvice接口进行判断
                request.setAttribute(RESPONSE_RESULT_ANN, clazz.getAnnotation(ResponseResult.class));
            } else if (method.isAnnotationPresent(ResponseResult.class)) {
                request.setAttribute(RESPONSE_RESULT_ANN, method.getAnnotation(ResponseResult.class));
            }

            boolean releaseToken = clazz.isAnnotationPresent(ReleaseToken.class)
                    || method.isAnnotationPresent(ReleaseToken.class);

            // 未显式放行的接口必须按照当前应用类型执行登录校验。
            if ("user".equals(appName) && !releaseToken) {
                StpUtil.checkLogin();
            }

            if ("admin".equals(appName) && !releaseToken) {
                StpAdminUtil.checkLogin();
            }
        }
        return true;
    }
}
