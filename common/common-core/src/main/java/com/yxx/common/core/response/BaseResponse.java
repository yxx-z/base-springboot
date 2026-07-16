package com.yxx.common.core.response;

import com.yxx.common.enums.ApiCode;
import lombok.Getter;
import lombok.Setter;


/**
 * 统一 API 响应。
 *
 * @param <T> 响应数据类型
 */
@Getter
public class BaseResponse<T> {

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
        // 统一在工厂方法内设置成功码，调用方只需要关注实际响应数据和链路标识。
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
        // 失败响应不携带业务数据，避免调用方误把部分结果当作成功结果消费。
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
