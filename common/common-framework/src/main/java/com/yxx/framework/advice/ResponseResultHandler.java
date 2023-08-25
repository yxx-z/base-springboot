package com.yxx.framework.advice;

import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.core.response.BaseResponse;
import com.yxx.common.core.response.ErrorResponse;
import com.yxx.framework.context.AppContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author yxx
 * @description: 使用 @ControllerAdvice & ResponseBodyAdvice 拦截Controller方法默认返回参数，统一处理返回值/响应体
 */
@Slf4j
@ControllerAdvice
public class ResponseResultHandler implements ResponseBodyAdvice<Object> {

    /**
     * 标记名称
     */
    public static final String RESPONSE_RESULT_ANN = "RESPONSE-RESULT";


    /**
     * 判断是否要执行 beforeBodyWrite 方法，true为执行，false不执行，有注解标记的时候处理返回值
     *
     * @param arg0 the return type
     * @param arg1 the selected converter type
     * @return return
     */
    @Override
    public boolean supports(@NotNull MethodParameter arg0, @NotNull Class<? extends HttpMessageConverter<?>> arg1) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert sra != null;
        HttpServletRequest request = sra.getRequest();
        // 判断请求是否有包装标记
        ResponseResult responseResultAnn = (ResponseResult) request.getAttribute(RESPONSE_RESULT_ANN);
        return responseResultAnn != null;
    }


    /**
     * 对返回值做包装处理，如果属于异常结果，则需要再包装
     *
     * @param body the body to be written
     * @param arg1 the return type of the controller method
     * @param arg2 the content type selected through content negotiation
     * @param arg3 the converter type selected to write to the response
     * @param arg4 the current request
     * @param arg5 the current response
     * @return the body that was passed in or a modified (possibly new) instance
     */
    @Override
    public Object beforeBodyWrite(Object body, @NotNull MethodParameter arg1, @NotNull MediaType arg2,
                                  @NotNull Class<? extends HttpMessageConverter<?>> arg3,
                                  @NotNull ServerHttpRequest arg4, @NotNull ServerHttpResponse arg5) {
        String traceId = AppContext.getContext().getTraceId();
        if (body instanceof ErrorResponse error) {
            return BaseResponse.fail(error.getCode(), error.getMessage(), traceId);
        } else if (body instanceof BaseResponse baseResponse) {
            baseResponse.setTraceId(traceId);
            return body;
        } else if (body instanceof String) {
            return BaseResponse.success(body, traceId);
        }

        return BaseResponse.success(body, traceId);
    }
}
