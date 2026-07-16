package com.yxx.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 账号输入规范化测试。 */
class AccountNormalizerTest {

    @Test
    void shouldNormalizeAccountAndEmailConsistently() {
        assertEquals("framework", AccountNormalizer.normalizeLoginCode(" Framework "));
        assertEquals("user@example.com", AccountNormalizer.normalizeEmail(" User@Example.COM "));
        assertEquals("显示 名称", AccountNormalizer.normalizeDisplayName("  显示 名称  "));
        assertNull(AccountNormalizer.normalizeMainlandPhone("   "));
    }
}
