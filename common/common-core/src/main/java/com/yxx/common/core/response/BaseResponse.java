package com.yxx.common.core.response;

import com.yxx.common.enums.ApiCode;
import lombok.Data;

import java.io.Serializable;

/**
 * @author yxx
 * @description: 返回结果实体类
 */
@Data
public class BaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 返回码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private Object data;

    private String traceId;

    private BaseResponse() {

    }

    public BaseResponse(ApiCode apiCode, Object data) {
        this.code = apiCode.code();
        this.message = apiCode.message();
        this.data = data;
    }

    private void setResultCode(ApiCode apiCode) {
        this.code = apiCode.code();
        this.message = apiCode.message();
    }

    /**
     * 返回数据
     *
     * @return 返回成功
     */
    public static BaseResponse success() {
        BaseResponse result = new BaseResponse();
        result.setResultCode(ApiCode.SUCCESS);
        return result;
    }

    /**
     * 返回成功
     *
     * @param data 自定义返回结果
     * @return 返回成功
     */
    public static BaseResponse success(Object data, String traceId) {
        BaseResponse result = new BaseResponse();
        result.setResultCode(ApiCode.SUCCESS);
        result.setData(data);
        result.setTraceId(traceId);
        return result;
    }

    public static BaseResponse success(Object data) {
        BaseResponse result = new BaseResponse();
        result.setResultCode(ApiCode.SUCCESS);
        result.setData(data);
        return result;
    }

    /**
     * 返回失败
     *
     * @param code    状态码
     * @param message 描述信息
     * @return 返回失败结果
     */
    public static BaseResponse fail(Integer code, String message, String traceId) {
        BaseResponse result = new BaseResponse();
        result.setCode(code);
        result.setMessage(message);
        result.setTraceId(traceId);
        return result;
    }

    public static BaseResponse fail(Integer code, String message) {
        BaseResponse result = new BaseResponse();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 返回失败
     *
     * @param apiCode 状态枚举类
     * @return 返回失败结果
     */
    public static BaseResponse fail(ApiCode apiCode, String traceId) {
        BaseResponse result = new BaseResponse();
        result.setResultCode(apiCode);
        result.setTraceId(traceId);
        return result;
    }

    public static BaseResponse fail(ApiCode apiCode) {
        BaseResponse result = new BaseResponse();
        result.setResultCode(apiCode);
        return result;
    }
}