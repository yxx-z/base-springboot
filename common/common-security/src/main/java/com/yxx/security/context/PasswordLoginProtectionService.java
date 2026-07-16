package com.yxx.security.context;

import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.utils.redis.RedissonCache;
import com.yxx.security.properties.LoginProtectionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 密码登录失败保护服务。
 *
 * <p>同时按安全域、账号和客户端 IP 统计失败次数，既限制针对单个账号的撞库，也限制单个
 * 来源批量枚举账号。Redis Key 中只保存账号摘要，避免直接暴露登录账号。</p>
 */
@Component
@RequiredArgsConstructor
public class PasswordLoginProtectionService {

    private static final String KEY_PREFIX = "security:login-failure:";

    private final RedissonCache redissonCache;
    private final LoginProtectionProperties properties;

    /**
     * 在执行密码校验前原子预占一次尝试。认证失败时保留计数，认证成功时必须调用
     * {@link #recordSuccess(String, String, String)} 归还本次预占并清除账号失败记录。
     */
    public void reserveAttempt(String realm, String account, String ip) {
        // 配置错误时至少保留一秒窗口，防止生成永不过期或无效的限流记录。
        long windowSeconds = Math.max(1L, properties.getWindow().toSeconds());
        // 密码比对之前完成原子预占，堵住大量请求同时通过“先检查、后计数”的并发窗口。
        boolean reserved = redissonCache.reserveLoginAttempt(
                accountKey(realm, account), properties.getAccountMaxFailures(),
                ipKey(realm, ip), properties.getIpMaxFailures(), windowSeconds);
        if (!reserved) {
            // 对账号阈值和 IP 阈值使用同一外部错误，避免向攻击者暴露触发维度。
            throw new ApiException(ApiCode.LOGIN_TOO_FREQUENT);
        }
    }

    /** 密码认证成功后清除当前账号的失败记录，IP 总体失败记录保留至窗口自然过期。 */
    public void recordSuccess(String realm, String account, String ip) {
        // 只在凭证验证全部成功后调用，认证异常时保留预占即等价于记录一次失败。
        redissonCache.completeSuccessfulLoginAttempt(
                accountKey(realm, account), ipKey(realm, ip));
    }

    private String accountKey(String realm, String account) {
        // 安全域参与 Key 组成，确保用户端与管理端同名账号互不影响。
        return KEY_PREFIX + realm + ":account:" + sha256(normalize(account));
    }

    private String ipKey(String realm, String ip) {
        return KEY_PREFIX + realm + ":ip:" + sha256(normalize(ip));
    }

    private String normalize(String value) {
        // 缺失值归入统一 unknown 桶，既避免空指针，也防止伪造空值绕过 IP/账号维度统计。
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            // Java 17 必须提供 SHA-256；如果运行时缺失，属于无法安全继续的环境错误。
            throw new IllegalStateException("当前 Java 运行时不支持 SHA-256", exception);
        }
    }
}
