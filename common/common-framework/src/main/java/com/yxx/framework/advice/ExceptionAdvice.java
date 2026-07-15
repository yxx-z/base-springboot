package com.yxx.framework.advice;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.yxx.common.core.response.ErrorResponse;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;


/**
 * @author yxx
 * @description: 全局异常处理类
 */
@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {
    private static final String ERROR_CODE = "发生业务异常！原因是: {}";
    private static final String LOGIN_ERROR_CODE = "发生登录异常！原因是: {},被封禁账号id: {}";
    private static final String UN_KNOW_ERROR_CODE = "发生未知异常！原因是: {}";
    private static final String CHECK_PARAMETERS_ERROR_CODE = "发生参数校验异常！原因是：{}";
    private static final String CHECK_PARAMETERS_ERROR = "发生参数校验异常！原因是：";
    private static final String REQUEST_ERROR_CODE = "发生请求异常！原因是：{}";

    /**
     * 处理自定义的业务异常
     *
     * @param e 异常对象
     * @return 错误结果
     */
    @ExceptionHandler(ApiException.class)
    public ErrorResponse bizExceptionHandler(ApiException e) {
        log.error(ERROR_CODE, e.getMessage());
        return ErrorResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public ErrorResponse notLoginException(NotLoginException e) {
        log.error(ERROR_CODE, e.getMessage());
        return switch (e.getType()) {
            case NotLoginException.NOT_TOKEN,
                 NotLoginException.INVALID_TOKEN,
                 NotLoginException.TOKEN_TIMEOUT,
                 NotLoginException.BE_REPLACED -> ErrorResponse.fail(ApiCode.TOKEN_ERROR.getCode(), e.getMessage());
            default -> ErrorResponse.fail(Integer.parseInt(e.getType()), e.getMessage());
        };
    }

    @ExceptionHandler(NotRoleException.class)
    public ErrorResponse notRoleException(NotRoleException e) {
        log.error(ERROR_CODE, e.getMessage());
        return ErrorResponse.fail(10000, e.getMessage());
    }

    @ExceptionHandler(DisableServiceException.class)
    public ErrorResponse disableLoginException(DisableServiceException e) {
        log.error(LOGIN_ERROR_CODE, e.getMessage(), e.getLoginId());
        long disableTime = e.getDisableTime();
        long day = disableTime / 86400L;
        long hour = (disableTime % 86400) / 3600L;
        long minute = (disableTime % 86400) % 3600L / 60L;
        long second = (disableTime % 86400) % 3600L % 60L;
        return ErrorResponse.fail(10000, e.getMessage() + ",距离解封还剩:" + day + "天" + hour + "时"
                + minute + "分" + second + "秒");
    }

    /**
     * 拦截抛出的异常
     *
     * @param e 异常
     * @return 拦截抛出的异常
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    public ErrorResponse handlerThrowable(Throwable e) {
        log.error(UN_KNOW_ERROR_CODE, e.getMessage());
        return ErrorResponse.fail(ApiCode.SYSTEM_ERROR, e);
    }

    /**
     * 参数校验异常
     *
     * @param e 异常
     * @return 参数校验异常返回
     */
    @ExceptionHandler(BindException.class)
    public ErrorResponse bindException(BindException e) {
        log.error(CHECK_PARAMETERS_ERROR, e);
        return ErrorResponse.fail(ApiCode.PARAM_IS_INVALID, e, e.getAllErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse constraintViolationException(ConstraintViolationException e) {
        log.error(CHECK_PARAMETERS_ERROR_CODE, e.getConstraintViolations().stream().iterator().next().getMessage());
        return ErrorResponse.fail(ApiCode.PARAM_IS_INVALID, e, e.getConstraintViolations().iterator().next().getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(CHECK_PARAMETERS_ERROR_CODE, e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        return ErrorResponse.fail(ApiCode.PARAM_IS_INVALID, e, e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ErrorResponse maxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error(REQUEST_ERROR_CODE, e.getMessage());
        return ErrorResponse.fail(ApiCode.PARAM_IS_INVALID, e, e.getMessage());
    }
}
