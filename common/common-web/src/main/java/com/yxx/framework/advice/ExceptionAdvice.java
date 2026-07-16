package com.yxx.framework.advice;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.yxx.common.core.response.ErrorResponse;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器。
 *
 * <p>业务错误码用于前端识别具体场景，HTTP 状态码用于网关、监控和通用客户端判断请求结果，
 * 两者职责不同，不能再使用 HTTP 200 承载所有失败响应。</p>
 */
@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    /**
     * 处理可预期的业务异常。
     *
     * @param exception 业务异常
     * @return 400 错误响应
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        log.warn("业务处理失败，code={}，message={}", exception.getCode(), exception.getMessage());
        return response(ApiHttpStatusMapper.resolve(exception.getCode()),
                ErrorResponse.fail(exception.getCode(), exception.getMessage()));
    }

    /**
     * 处理未登录、Token 无效和 Token 过期。
     *
     * @param exception 登录异常
     * @return 401 错误响应
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ErrorResponse> handleNotLoginException(NotLoginException exception) {
        log.warn("认证失败：{}", exception.getMessage());
        return response(HttpStatus.UNAUTHORIZED,
                ErrorResponse.fail(ApiCode.TOKEN_ERROR.code(), ApiCode.TOKEN_ERROR.message()));
    }

    /**
     * 处理角色或权限不足。
     *
     * @param exception 鉴权异常
     * @return 403 错误响应
     */
    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<ErrorResponse> handleForbiddenException(RuntimeException exception) {
        log.warn("授权失败：{}", exception.getMessage());
        return response(HttpStatus.FORBIDDEN,
                ErrorResponse.fail(ApiCode.USER_PERMISSION_ERROR.code(), ApiCode.USER_PERMISSION_ERROR.message()));
    }

    /**
     * 处理账号被封禁。
     *
     * @param exception 封禁异常
     * @return 403 错误响应
     */
    @ExceptionHandler(DisableServiceException.class)
    public ResponseEntity<ErrorResponse> handleDisableServiceException(DisableServiceException exception) {
        log.warn("账号已被封禁，loginId={}，剩余秒数={}", exception.getLoginId(), exception.getDisableTime());
        return response(HttpStatus.FORBIDDEN,
                ErrorResponse.fail(ApiCode.USER_PERMISSION_ERROR.code(), exception.getMessage()));
    }

    /**
     * 处理请求体和表单参数校验失败。
     *
     * @param exception 参数绑定异常
     * @return 400 错误响应
     */
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleBindException(BindException exception) {
        String message = exception.getBindingResult().getAllErrors().isEmpty()
                ? ApiCode.PARAM_IS_INVALID.message()
                : exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验失败：{}", message);
        return response(HttpStatus.BAD_REQUEST, ErrorResponse.fail(ApiCode.PARAM_IS_INVALID.code(), message));
    }

    /**
     * 处理方法参数约束异常。
     *
     * @param exception 约束异常
     * @return 400 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ApiCode.PARAM_IS_INVALID.message());
        log.warn("参数约束校验失败：{}", message);
        return response(HttpStatus.BAD_REQUEST, ErrorResponse.fail(ApiCode.PARAM_IS_INVALID.code(), message));
    }

    /**
     * 处理上传文件过大。
     *
     * @param exception 上传异常
     * @return 413 错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception) {
        log.warn("上传内容超过限制：{}", exception.getMessage());
        return response(HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorResponse.fail(ApiCode.PARAM_IS_INVALID.code(), "上传内容超过系统限制"));
    }

    /**
     * 处理数据库唯一约束、外键约束等完整性冲突。
     *
     * @param exception 数据完整性异常
     * @return 409 错误响应
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception) {
        log.warn("数据完整性约束冲突：{}", exception.getMostSpecificCause().getMessage());
        return response(HttpStatus.CONFLICT,
                ErrorResponse.fail(ApiCode.USER_EXIST.code(), "数据已存在或关联关系不合法"));
    }

    /**
     * 兜底处理未知异常。完整异常堆栈只写入服务端日志，响应中不暴露实现类和内部信息。
     *
     * @param throwable 未知异常
     * @return 500 错误响应
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleThrowable(Throwable throwable) {
        log.error("发生未处理异常", throwable);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorResponse.fail(ApiCode.SYSTEM_ERROR.code(), ApiCode.SYSTEM_ERROR.message()));
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, ErrorResponse body) {
        return ResponseEntity.status(status).body(body);
    }

}
