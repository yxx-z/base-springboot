package com.yxx.common.exceptions;

import com.yxx.common.enums.ApiCode;

/**
 * @author yxx
 * @description: 自定义异常类
 */
public class ApiException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    public ApiException(ApiCode resultCode) {
        super(resultCode.message());
        this.code = resultCode.code();
    }

    public ApiException(ApiCode resultCode, Throwable cause) {
        super(resultCode.message(), cause);
        this.code = resultCode.code();
    }

    public ApiException(String message) {
        super(message);
        this.code = -1;
    }

    public ApiException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

}
