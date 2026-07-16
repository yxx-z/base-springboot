package com.yxx.security.context;

import cn.dev33.satoken.temp.SaTempUtil;
import com.yxx.common.utils.redis.RedissonCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.function.Function;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Sa-Token 临时令牌的一次性消费协调器。
 *
 * <p>通过 Redis 原子占位防止同一个重置密码链接被并发提交。事务提交后才真正删除临时令牌；
 * 如果数据库事务回滚，则释放占位并保留原令牌，用户可以再次提交。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OneTimeTemporaryTokenService {

    private static final String RESERVATION_PREFIX = "security:temporary-token:reservation:";

    private final RedissonCache redissonCache;

    /**
     * 尝试在当前事务中独占消费临时令牌。
     *
     * @param token 临时令牌
     * @param payloadDecoder 版本化字符串载荷解码器
     * @return 令牌有效且成功取得消费权时返回载荷，否则返回空
     */
    public <T> Optional<T> reserve(String token,
                                   Function<String, Optional<T>> payloadDecoder) {
        // 先读取原令牌剩余寿命；-2 是 Sa-Token 对“不存在或已失效”的约定值。
        long timeout = SaTempUtil.getTimeout(token);
        if (timeout == -2) {
            return Optional.empty();
        }

        // 永久临时令牌也只给占位设置有限存活时间，避免进程异常退出后形成永久死锁。
        long reservationSeconds = timeout > 0 ? timeout : 15 * 60L;
        // Redis Key 只保存摘要，避免令牌通过监控、Key 扫描或运维日志泄露。
        String reservationKey = RESERVATION_PREFIX + sha256(token);
        // SET NX 决定唯一消费者；并发请求取得失败后直接按令牌无效处理。
        if (!redissonCache.putStringIfAbsent(reservationKey, "reserved", reservationSeconds)) {
            return Optional.empty();
        }

        Optional<T> payload;
        try {
            // 只有成功预占后才解析载荷，避免多个并发请求同时进入后续数据库事务。
            String serializedPayload = SaTempUtil.parseToken(token, String.class);
            payload = payloadDecoder.apply(serializedPayload);
        } catch (RuntimeException exception) {
            // 解析异常不应永久占用令牌，释放后仍允许合法请求重试。
            redissonCache.remove(reservationKey);
            return Optional.empty();
        }
        if (payload.isEmpty()) {
            // 版本不兼容或载荷校验失败同样不消费原令牌。
            redissonCache.remove(reservationKey);
            return Optional.empty();
        }

        registerCompletion(token, reservationKey, reservationSeconds);
        return payload;
    }

    private void registerCompletion(String token, String reservationKey, long reservationSeconds) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            // 无数据库事务时当前方法边界即为成功边界，可以立即完成消费。
            markConsumedAndDeleteToken(token, reservationKey, reservationSeconds);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 数据落库后再删除令牌，避免业务回滚却无法再次使用重置链接。
                markConsumedAndDeleteToken(token, reservationKey, reservationSeconds);
            }

            @Override
            public void afterCompletion(int status) {
                // 只有事务回滚才释放占位。提交后即使删除原 Token 失败，也必须保留“已消费”
                // 标记至原 Token 自然过期，防止同一个重置链接再次提交。
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    redissonCache.remove(reservationKey);
                }
            }
        });
    }

    private void markConsumedAndDeleteToken(String token,
                                            String reservationKey,
                                            long reservationSeconds) {
        // 先把预占状态升级为已消费，再删除原令牌；即使第二步异常也不会出现重复消费窗口。
        redissonCache.putString(reservationKey, "consumed", reservationSeconds);
        try {
            SaTempUtil.deleteToken(token);
        } catch (RuntimeException exception) {
            // reservation 已经阻止令牌再次消费。删除失败需要运维关注，但不能恢复消费资格。
            log.error("删除已消费的临时令牌失败，tokenHash={}", sha256(token), exception);
        }
    }

    private String sha256(String value) {
        try {
            // SHA-256 仅用于不可逆 Key 标识，不承担密码存储职责。
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 运行时不支持 SHA-256", exception);
        }
    }
}
