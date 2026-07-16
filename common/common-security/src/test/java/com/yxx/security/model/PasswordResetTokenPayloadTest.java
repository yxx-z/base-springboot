package com.yxx.security.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证密码重置载荷的版本化字符串协议及非法输入拒绝行为。 */
class PasswordResetTokenPayloadTest {

    @Test
    void shouldRoundTripPayloadWithoutDependingOnObjectSerialization() {
        PasswordResetTokenPayload payload = new PasswordResetTokenPayload(
                "user", 42L, "user.name+tag@example.com");

        assertEquals(payload, PasswordResetTokenPayload.decode(payload.encode()).orElseThrow());
    }

    @Test
    void shouldRejectUnsupportedOrMalformedPayload() {
        assertTrue(PasswordResetTokenPayload.decode("v2.invalid.1.invalid").isEmpty());
        assertTrue(PasswordResetTokenPayload.decode("v1.invalid.not-a-number.invalid").isEmpty());
        assertTrue(PasswordResetTokenPayload.decode("").isEmpty());
    }
}
