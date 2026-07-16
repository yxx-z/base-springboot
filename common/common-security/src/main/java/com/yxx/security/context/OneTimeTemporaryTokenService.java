package com.yxx.security.context;

import cn.dev33.satoken.temp.SaTempUtil;
import com.yxx.common.utils.redis.RedissonCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
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
     * @param payloadType 令牌载荷类型
     * @return 令牌有效且成功取得消费权时返回载荷，否则返回空
     */
    public <T> Optional<T> reserve(String token, Class<T> payloadType) {
        long timeout = SaTempUtil.getTimeout(token);
        if (timeout == -2) {
            return Optional.empty();
        }

        // 永久临时令牌也只给占位设置有限存活时间，避免进程异常退出后形成永久死锁。
        long reservationSeconds = timeout > 0 ? timeout : 15 * 60L;
        String reservationKey = RESERVATION_PREFIX + sha256(token);
        if (!redissonCache.putStringIfAbsent(reservationKey, "reserved", reservationSeconds)) {
            return Optional.empty();
        }

        T payload;
        try {
            payload = SaTempUtil.parseToken(token, payloadType);
        } catch (RuntimeException exception) {
            redissonCache.remove(reservationKey);
            return Optional.empty();
        }
        if (payload == null) {
            redissonCache.remove(reservationKey);
            return Optional.empty();
        }

        registerCompletion(token, reservationKey, reservationSeconds);
        return Optional.of(payload);
    }

    private void registerCompletion(String token, String reservationKey, long reservationSeconds) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            markConsumedAndDeleteToken(token, reservationKey, reservationSeconds);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
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
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 运行时不支持 SHA-256", exception);
        }
    }
}
