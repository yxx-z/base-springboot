package com.yxx.framework.interceptor.response;

import com.yxx.common.annotation.response.ResponseResult;
import org.springframework.lang.NonNull;
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
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            // 仅 Controller 方法具有可检查的类与方法注解，静态资源直接跳过。
            final Class<?> clazz = handlerMethod.getBeanType();
            final Method method = handlerMethod.getMethod();

            // 类级注解优先，适合整个 Controller 采用统一响应协议。
            if (clazz.isAnnotationPresent(ResponseResult.class)) {
                // 通过请求属性把决定传递给 ResponseBodyAdvice，避免后续再次反射查找。
                request.setAttribute(RESPONSE_RESULT_ANN, clazz.getAnnotation(ResponseResult.class));
            } else if (method.isAnnotationPresent(ResponseResult.class)) {
                // 未标注类时允许单个方法选择启用，保持框架默认不强制包装。
                request.setAttribute(RESPONSE_RESULT_ANN, method.getAnnotation(ResponseResult.class));
            }
        }
        return true;
    }
}
