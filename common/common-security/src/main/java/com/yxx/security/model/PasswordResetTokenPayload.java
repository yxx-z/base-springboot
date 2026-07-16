package com.yxx.security.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * 密码重置临时令牌载荷。
 *
 * <p>令牌同时绑定安全域、内部主体 ID 和邮箱，防止管理端与用户端在共享 Redis 时互相使用
 * 对方生成的重置链接，也避免邮箱变更后令牌错误作用于其他主体。</p>
 */
public record PasswordResetTokenPayload(String realm, Long subjectId, String email) {

    private static final String VERSION = "v1";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    /**
     * 将载荷编码为与 Redis 序列化器无关的版本化字符串。
     *
     * <p>Sa-Token 的临时 Token 类型转换只保证字符串和基础类型可稳定恢复，因此不能直接
     * 保存 record、Map 等复杂对象。安全域和邮箱使用 Base64 URL 编码，避免分隔符冲突；
     * 明文内容本身不承担保密职责，真正对外暴露的仍是随机临时 Token。</p>
     */
    public String encode() {
        return String.join(".", VERSION, encodePart(realm), String.valueOf(subjectId), encodePart(email));
    }

    /**
     * 解码版本化载荷。格式错误、版本不支持或字段不完整时返回空，由调用方统一视为无效链接。
     */
    public static Optional<PasswordResetTokenPayload> decode(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String[] parts = value.split("\\.", 4);
        if (parts.length != 4 || !VERSION.equals(parts[0])) {
            return Optional.empty();
        }
        try {
            String realm = decodePart(parts[1]);
            Long subjectId = Long.valueOf(parts[2]);
            String email = decodePart(parts[3]);
            if (realm.isBlank() || subjectId <= 0 || email.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new PasswordResetTokenPayload(realm, subjectId, email));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static String encodePart(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePart(String value) {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }
}
