package com.yxx.framework.interceptor.response;

import cn.dev33.satoken.stp.StpUtil;
import com.yxx.common.annotation.auth.ReleaseToken;
import com.yxx.common.annotation.response.ResponseResult;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                             @NotNull Object handler) {
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

            // 判断方法上是否加了放行token校验的注解
            if (!method.isAnnotationPresent(ReleaseToken.class)) {
                StpUtil.checkLogin();
            }
        }
        return true;
    }
}
