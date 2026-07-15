package com.yxx.security.model;

import java.io.Serializable;

/**
 * 密码重置临时令牌载荷。
 *
 * <p>令牌同时绑定安全域、内部主体 ID 和邮箱，防止管理端与用户端在共享 Redis 时互相使用
 * 对方生成的重置链接，也避免邮箱变更后令牌错误作用于其他主体。</p>
 */
public record PasswordResetTokenPayload(String realm, Long subjectId, String email) implements Serializable {
}
