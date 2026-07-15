package com.yxx.common.utils.encryptor;

import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 字段加密实现。
 *
 * <p>每次加密使用独立随机盐和随机 IV，并通过 PBKDF2 从配置密钥派生 AES-256 密钥。
 * GCM 同时提供机密性与完整性校验，密文被修改时解密会明确失败。</p>
 */
public class AesGcmEncryptor implements IEncryptor {

    private static final String VERSION = "v1";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int ITERATIONS = 120_000;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String encrypt(Object value, String key) {
        validate(value, key);
        byte[] salt = randomBytes(SALT_LENGTH);
        byte[] iv = randomBytes(IV_LENGTH);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(key, salt),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(value.toString().getBytes(StandardCharsets.UTF_8));

            ByteBuffer payload = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
            payload.put(salt).put(iv).put(cipherText);
            return VERSION + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
        } catch (GeneralSecurityException exception) {
            throw new ApiException(ApiCode.ENCRYPTION_ERROR, exception);
        }
    }

    @Override
    public String decrypt(Object value, String key) {
        validate(value, key);
        String encryptedValue = value.toString();
        if (!encryptedValue.startsWith(VERSION + ":")) {
            throw new ApiException(ApiCode.DECODE_ERROR);
        }

        try {
            byte[] payload = Base64.getUrlDecoder().decode(encryptedValue.substring(VERSION.length() + 1));
            if (payload.length <= SALT_LENGTH + IV_LENGTH) {
                throw new ApiException(ApiCode.DECODE_ERROR);
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[payload.length - SALT_LENGTH - IV_LENGTH];
            buffer.get(salt).get(iv).get(cipherText);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(key, salt),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (ApiException exception) {
            throw exception;
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new ApiException(ApiCode.DECODE_ERROR, exception);
        }
    }

    private SecretKeySpec deriveKey(String key, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec keySpec = new PBEKeySpec(key.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            byte[] encoded = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
                    .generateSecret(keySpec)
                    .getEncoded();
            return new SecretKeySpec(encoded, "AES");
        } finally {
            keySpec.clearPassword();
        }
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private void validate(Object value, String key) {
        if (value == null) {
            throw new ApiException(ApiCode.PARAM_IS_BLANK);
        }
        if (key == null || key.length() < 12) {
            throw new ApiException(ApiCode.KEY_LENGTH_ERROR);
        }
    }
}
