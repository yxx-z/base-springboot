package com.yxx.framework.advice;

import com.yxx.common.enums.ApiCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 业务错误码与 HTTP 状态映射测试。 */
class ApiHttpStatusMapperTest {

    @Test
    void shouldMapSecurityAndConcurrencyErrors() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                ApiHttpStatusMapper.resolve(ApiCode.AUTHENTICATION_FAILED.code()));
        assertEquals(HttpStatus.FORBIDDEN,
                ApiHttpStatusMapper.resolve(ApiCode.LAST_SUPER_ADMIN.code()));
        assertEquals(HttpStatus.CONFLICT,
                ApiHttpStatusMapper.resolve(ApiCode.USER_EXIST.code()));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                ApiHttpStatusMapper.resolve(ApiCode.LOGIN_TOO_FREQUENT.code()));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                ApiHttpStatusMapper.resolve(ApiCode.SYSTEM_ERROR.code()));
    }
}
