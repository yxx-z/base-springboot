package com.yxx.framework.advice;

import com.yxx.common.enums.ApiCode;
import org.springframework.http.HttpStatus;

/** 将业务错误码映射为标准 HTTP 状态码。 */
public final class ApiHttpStatusMapper {

    private ApiHttpStatusMapper() {
    }

    public static HttpStatus resolve(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (code.equals(ApiCode.TOKEN_ERROR.code())
                || code.equals(ApiCode.USER_NOT_LOGIN.code())
                || code.equals(ApiCode.AUTHENTICATION_FAILED.code())) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code.equals(ApiCode.ACCOUNT_DISABLED.code())
                || code.equals(ApiCode.USER_PERMISSION_ERROR.code())
                || code.equals(ApiCode.USER_NOT_ROLE.code())
                || code.equals(ApiCode.IDENTITY_DISABLED.code())
                || code.equals(ApiCode.IDENTITY_NOT_VERIFIED.code())
                || code.equals(ApiCode.LAST_SUPER_ADMIN.code())
                || code.equals(ApiCode.BUILT_IN_ROLE_IMMUTABLE.code())) {
            return HttpStatus.FORBIDDEN;
        }
        if (code.equals(ApiCode.USER_EXIST.code())
                || code.equals(ApiCode.EMAIL_EXIST.code())) {
            return HttpStatus.CONFLICT;
        }
        if (code.equals(ApiCode.LOGIN_TOO_FREQUENT.code())
                || code.equals(ApiCode.RESET_PWD_MAX.code())
                || code.equals(ApiCode.REGISTER_MAX.code())) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code.equals(ApiCode.SYSTEM_ERROR.code())) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
