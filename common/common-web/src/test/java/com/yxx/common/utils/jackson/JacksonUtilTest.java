package com.yxx.common.utils.jackson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 公共 JSON 语义测试。 */
class JacksonUtilTest {

    @Test
    void shouldPreserveNullAndIgnoreUnknownProperties() {
        String json = JacksonUtil.toJson(new Sample(null));
        assertTrue(json.contains("\"value\":null"));
        assertDoesNotThrow(() -> JacksonUtil.readValue(
                "{\"value\":\"ok\",\"unknown\":1}", Sample.class));
    }

    @Test
    void shouldNotHideInvalidJson() {
        assertThrows(IllegalArgumentException.class,
                () -> JacksonUtil.readValue("not-json", Sample.class));
    }

    private record Sample(String value) {
    }
}
