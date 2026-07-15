package com.yxx.framework.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一密码编码策略测试。
 */
class PasswordConfigTest {

    @Test
    void shouldEncodeAndMatchPasswordWithBcrypt() {
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
        String encoded = encoder.encode("StrongPassword123!");

        assertNotEquals("StrongPassword123!", encoded);
        assertTrue(encoder.matches("StrongPassword123!", encoded));
    }
}
