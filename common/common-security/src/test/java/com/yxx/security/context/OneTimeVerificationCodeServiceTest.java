package com.yxx.security.context;

import com.yxx.common.utils.redis.RedissonCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 一次性验证码预占结果测试。 */
class OneTimeVerificationCodeServiceTest {

    @Test
    void shouldDistinguishMissingInvalidAndReservedCode() {
        RedissonCache cache = mock(RedissonCache.class);
        OneTimeVerificationCodeService service = new OneTimeVerificationCodeService(cache);

        when(cache.reserveStringIfEquals(eq("captcha:key"), eq("123456"), anyString()))
                .thenReturn(-1L, 0L, 1L);

        assertEquals(OneTimeVerificationCodeService.ReservationResult.NOT_FOUND,
                service.reserve("captcha:key", "123456"));
        assertEquals(OneTimeVerificationCodeService.ReservationResult.NOT_MATCHED,
                service.reserve("captcha:key", "123456"));
        assertEquals(OneTimeVerificationCodeService.ReservationResult.RESERVED,
                service.reserve("captcha:key", "123456"));
        verify(cache).deleteStringIfEquals(eq("captcha:key"), anyString());
    }
}
