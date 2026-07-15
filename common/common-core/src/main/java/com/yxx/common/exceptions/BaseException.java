package com.yxx.common.exceptions;

import com.yxx.common.enums.ApiCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 基础异常
 *
 * @author yxx
 * @since 2022/7/13 18:19
 */
@Getter
public class BaseException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 2588129946203384980L;

    private Integer code;

    public BaseException(Integer code) {
        super();
        this.code = code;
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(ApiCode apiCode) {
        super(apiCode.getMessage());
        this.code = apiCode.getCode();
    }

    public BaseException(Throwable cause) {
        super(cause);
    }
}
