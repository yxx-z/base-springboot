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
        // 在执行高成本密钥派生前完成输入校验，尽早拒绝空值和强度不足的配置密钥。
        validate(value, key);
        // 每次加密都生成独立盐和 IV；重复明文也不会得到相同密文。
        byte[] salt = randomBytes(SALT_LENGTH);
        byte[] iv = randomBytes(IV_LENGTH);
        try {
            // 使用本次随机盐派生 AES 密钥，并以 96 位 IV 初始化 GCM 推荐参数。
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(key, salt),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(value.toString().getBytes(StandardCharsets.UTF_8));

            // 密文载荷按“盐 + IV + GCM 密文及认证标签”排列，解密端可无状态恢复所需参数。
            ByteBuffer payload = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
            payload.put(salt).put(iv).put(cipherText);
            return VERSION + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
        } catch (GeneralSecurityException exception) {
            throw new ApiException(ApiCode.ENCRYPTION_ERROR, exception);
        }
    }

    @Override
    public String decrypt(Object value, String key) {
        // 解密与加密共享相同的输入约束，避免错误密钥进入底层加密 API 后产生不明确异常。
        validate(value, key);
        String encryptedValue = value.toString();
        // 版本前缀用于未来升级算法或载荷格式；未知版本禁止按当前协议猜测解析。
        if (!encryptedValue.startsWith(VERSION + ":")) {
            throw new ApiException(ApiCode.DECODE_ERROR);
        }

        try {
            // URL-safe Base64 只负责传输编码，解码失败统一映射为业务解码错误。
            byte[] payload = Base64.getUrlDecoder().decode(encryptedValue.substring(VERSION.length() + 1));
            if (payload.length <= SALT_LENGTH + IV_LENGTH) {
                throw new ApiException(ApiCode.DECODE_ERROR);
            }

            // 按固定长度拆出盐和 IV，剩余部分包含 GCM 密文与认证标签。
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[payload.length - SALT_LENGTH - IV_LENGTH];
            buffer.get(salt).get(iv).get(cipherText);

            // 使用相同 PBKDF2 参数重新派生密钥；密文或密钥不匹配会在 doFinal 校验标签时失败。
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
        // PBEKeySpec 使用 char[] 保存口令，finally 中主动清理，缩短敏感信息在内存中的存活时间。
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
        // SecureRandom 提供密码学安全随机数，不能替换为 Random 或固定 IV。
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
