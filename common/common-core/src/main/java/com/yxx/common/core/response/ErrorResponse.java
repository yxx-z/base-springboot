package com.yxx.common.core.response;

import com.yxx.common.enums.ApiCode;
import lombok.Data;

/**
 * 异常结果包装类
 *
 * @author yxx
 */
@Data
public class ErrorResponse {

    private Integer code;

    private String message;

    private String exception;

    /**
     * @param resultEnum 状态枚举类
     * @param e          异常
     * @param message    异常描述信息
     * @return 自定义异常返回
     */
    public static ErrorResponse fail(ApiCode apiCode, Throwable e, String message) {
        ErrorResponse errorResult = ErrorResponse.fail(apiCode, e);
        errorResult.setMessage(message);
        return errorResult;
    }

    /**
     * @param resultEnum 状态枚举类
     * @param e          异常
     * @return 自定义异常返回
     */
    public static ErrorResponse fail(ApiCode apiCode, Throwable e) {
        ErrorResponse errorResult = new ErrorResponse();
        errorResult.setCode(apiCode.code());
        errorResult.setMessage(apiCode.message());
        errorResult.setException(e.getClass().getName());
        return errorResult;
    }

    /**
     * @param code    异常状态码
     * @param message 异常描述信息
     * @return 自定义异常返回
     */
    public static ErrorResponse fail(Integer code, String message) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode(code);
        errorResponse.setMessage(message);
        return errorResponse;
    }
}