package com.yxx.framework.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.core.response.BaseResponse;
import com.yxx.common.core.response.ErrorResponse;
import com.yxx.framework.context.AppContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
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

    private final ObjectMapper objectMapper;

    public ResponseResultHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
    public boolean supports(MethodParameter arg0, Class<? extends HttpMessageConverter<?>> arg1) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) {
            return false;
        }
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
    public Object beforeBodyWrite(Object body, MethodParameter arg1, MediaType arg2,
                                  Class<? extends HttpMessageConverter<?>> arg3,
                                  ServerHttpRequest arg4, ServerHttpResponse arg5) {
        String traceId = AppContext.getTraceId();
        Object wrappedBody;
        if (body instanceof ErrorResponse error) {
            wrappedBody = BaseResponse.fail(error.getCode(), error.getMessage(), traceId);
        } else if (body instanceof BaseResponse<?> baseResponse) {
            baseResponse.setTraceId(traceId);
            wrappedBody = body;
        } else {
            wrappedBody = BaseResponse.success(body, traceId);
        }

        // StringHttpMessageConverter 只能写出字符串，需显式序列化统一响应对象。
        if (StringHttpMessageConverter.class.isAssignableFrom(arg3)) {
            arg5.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(wrappedBody);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("统一响应序列化失败", exception);
            }
        }
        return wrappedBody;
    }
}
