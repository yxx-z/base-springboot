package com.yxx.common.exceptions;

import com.yxx.common.enums.ApiCode;
import lombok.Data;

/**
 * @author yxx
 * @description: 自定义异常类
 */
@Data
public class ApiException extends RuntimeException {

  /**
     * 错误码
     */
  private Integer code;

  /**
     * 错误信息
     */
  private String message;

  public ApiException() {
    super();
  }

  public ApiException(ApiCode resultCode) {
    super(resultCode.message());
    this.code = resultCode.code();
    this.message = resultCode.message();
  }

  public ApiException(ApiCode resultCode, Throwable cause) {
    super(resultCode.message(), cause);
    this.code = resultCode.code();
    this.message = resultCode.message();
  }

  public ApiException(String message) {
    super(message);
    this.code = -1;
    this.message = message;
  }

  public ApiException(Integer code, String message) {
    super(message);
    this.code = code;
    this.message = message;
  }

  public ApiException(Integer code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.message = message;
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}