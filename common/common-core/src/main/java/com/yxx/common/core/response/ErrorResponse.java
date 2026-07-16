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

    /**
     * @param apiCode 状态枚举类
     * @param e       异常
     * @param message 异常描述信息
     * @return 自定义异常返回
     */
    public static ErrorResponse fail(ApiCode apiCode, Throwable e, String message) {
        // 先复用标准错误码构造，再仅覆盖确实需要对外展示的自定义消息。
        ErrorResponse errorResult = ErrorResponse.fail(apiCode, e);
        errorResult.setMessage(message);
        return errorResult;
    }

    /**
     * @param apiCode 状态枚举类
     * @param e       异常
     * @return 自定义异常返回
     */
    public static ErrorResponse fail(ApiCode apiCode, Throwable e) {
        // Throwable 只用于保持调用签名兼容，异常堆栈由统一异常处理器记录，不写入响应对象。
        ErrorResponse errorResult = new ErrorResponse();
        errorResult.setCode(apiCode.code());
        errorResult.setMessage(apiCode.message());
        return errorResult;
    }

    /**
     * @param code    异常状态码
     * @param message 异常描述信息
     * @return 自定义异常返回
     */
    public static ErrorResponse fail(Integer code, String message) {
        // 非枚举错误码场景由调用方显式提供码和消息，仍保持统一响应结构。
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode(code);
        errorResponse.setMessage(message);
        return errorResponse;
    }
}
