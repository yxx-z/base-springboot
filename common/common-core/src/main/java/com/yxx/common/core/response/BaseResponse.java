package com.yxx.common.core.response;

import com.yxx.common.enums.ApiCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一 API 响应。
 *
 * @param <T> 响应数据类型
 */
@Getter
public class BaseResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务状态码。 */
    private Integer code;
    /** 面向调用方的响应消息。 */
    private String message;
    /** 响应数据。 */
    private T data;
    /** 请求链路标识。 */
    @Setter
    private String traceId;

    private BaseResponse() {
    }

    public static BaseResponse<Void> success() {
        return success(null, null);
    }

    public static <T> BaseResponse<T> success(T data) {
        return success(data, null);
    }

    public static <T> BaseResponse<T> success(T data, String traceId) {
        BaseResponse<T> response = new BaseResponse<>();
        response.code = ApiCode.SUCCESS.code();
        response.message = ApiCode.SUCCESS.message();
        response.data = data;
        response.traceId = traceId;
        return response;
    }

    public static BaseResponse<Void> fail(Integer code, String message) {
        return fail(code, message, null);
    }

    public static BaseResponse<Void> fail(Integer code, String message, String traceId) {
        BaseResponse<Void> response = new BaseResponse<>();
        response.code = code;
        response.message = message;
        response.traceId = traceId;
        return response;
    }

    public static BaseResponse<Void> fail(ApiCode apiCode) {
        return fail(apiCode, null);
    }

    public static BaseResponse<Void> fail(ApiCode apiCode, String traceId) {
        return fail(apiCode.code(), apiCode.message(), traceId);
    }
}
