package com.yxx.common.utils.encryptor;

import com.yxx.common.exceptions.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AES-GCM 字段加密测试。
 */
class AesGcmEncryptorTest {

    private final AesGcmEncryptor encryptor = new AesGcmEncryptor();

    @Test
    void shouldDecryptEncryptedValue() {
        String key = "a-strong-test-key";
        String encrypted = encryptor.encrypt("敏感信息", key);

        assertNotEquals("敏感信息", encrypted);
        assertEquals("敏感信息", encryptor.decrypt(encrypted, key));
    }

    @Test
    void shouldGenerateDifferentCipherTextForSameValue() {
        String key = "a-strong-test-key";

        assertNotEquals(encryptor.encrypt("相同内容", key), encryptor.encrypt("相同内容", key));
    }

    @Test
    void shouldRejectTamperedCipherText() {
        String key = "a-strong-test-key";
        String encrypted = encryptor.encrypt("敏感信息", key);
        String encodedPayload = encrypted.substring(encrypted.indexOf(':') + 1);
        byte[] payload = Base64.getUrlDecoder().decode(encodedPayload);
        payload[payload.length - 1] ^= 0x01;
        String tampered = "v1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);

        assertThrows(ApiException.class, () -> encryptor.decrypt(tampered, key));
    }
}
