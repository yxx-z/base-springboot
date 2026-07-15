package com.yxx.security.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 登录主体序列化与不可变性测试。
 */
class LoginPrincipalTest {

    @Test
    void shouldRoundTripThroughJacksonForRedisSessionStorage() throws Exception {
        LoginPrincipal source = LoginPrincipal.builder()
                .subjectId(1L)
                .subjectType("user")
                .account("tester")
                .displayName("测试用户")
                .loginMode("password")
                .roles(Set.of("user:member"))
                .permissions(Set.of("user:profile:read"))
                .loginTime(LocalDateTime.of(2026, 7, 15, 12, 0))
                .build();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        LoginPrincipal restored = objectMapper.readValue(
                objectMapper.writeValueAsBytes(source), LoginPrincipal.class);

        assertEquals(source, restored);
    }

    @Test
    void shouldExposeUnmodifiableAuthorizationCollections() {
        LoginPrincipal principal = LoginPrincipal.builder()
                .roles(Set.of("user:member"))
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> principal.getRoles().add("user:administrator"));
    }
}
