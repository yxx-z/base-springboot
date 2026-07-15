package com.yxx.framework.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 日志敏感字段脱敏测试。
 */
class SensitiveDataSanitizerTest {

    private final SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer(new ObjectMapper());

    @Test
    void shouldMaskSensitiveObjectFields() {
        String result = sanitizer.sanitize(Map.of(
                "username", "tester",
                "password", "plain-password",
                "accessToken", "token-value"));

        assertTrue(result.contains("tester"));
        assertFalse(result.contains("plain-password"));
        assertFalse(result.contains("token-value"));
    }

    @Test
    void shouldMaskSensitiveJsonText() {
        String result = sanitizer.sanitizeText("{\"password\":\"123456\",\"name\":\"张三\"}");

        assertFalse(result.contains("123456"));
        assertTrue(result.contains("张三"));
    }
}
