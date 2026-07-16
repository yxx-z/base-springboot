package com.yxx.security.context;

import com.yxx.common.utils.redis.RedissonCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Redis 验证码的一次性事务消费协调器。
 *
 * <p>消费时先将验证码原子替换为随机预占标记。数据库事务提交后删除，回滚时恢复原验证码
 * 且保留剩余 TTL，从而同时保证并发唯一消费和数据库事务失败后的可重试性。</p>
 */
@Component
@RequiredArgsConstructor
public class OneTimeVerificationCodeService {

    private static final String RESERVATION_PREFIX = "reserved:";

    private final RedissonCache redissonCache;

    public ReservationResult reserve(String key, String verificationCode) {
        // 每次请求生成唯一标记，事务回调只能处理自己取得的预占，不能误操作其他请求。
        String reservation = RESERVATION_PREFIX + UUID.randomUUID();
        // 比较验证码和替换预占标记由 Redis 原子完成，保证并发提交只有一个成功者。
        long result = redissonCache.reserveStringIfEquals(key, verificationCode, reservation);
        if (result < 0) {
            // Key 不存在通常表示验证码未发送、已过期或已经成功消费。
            return ReservationResult.NOT_FOUND;
        }
        if (result == 0) {
            // 值不匹配既包括验证码错误，也包括已被另一请求预占。
            return ReservationResult.NOT_MATCHED;
        }
        registerCompletion(key, verificationCode, reservation);
        return ReservationResult.RESERVED;
    }

    private void registerCompletion(String key, String originalValue, String reservation) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            // 非事务场景下方法正常执行即视为消费完成，立即条件删除预占值。
            redissonCache.deleteStringIfEquals(key, reservation);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 业务数据提交后才真正删除验证码，确保事务失败时用户仍可重试。
                redissonCache.deleteStringIfEquals(key, reservation);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    // 回滚时仅在预占仍属于当前请求时恢复原值，并保留验证码原 TTL。
                    redissonCache.restoreReservedString(key, reservation, originalValue);
                }
            }
        });
    }

    /** 验证码预占结果。 */
    public enum ReservationResult {
        RESERVED,
        NOT_FOUND,
        NOT_MATCHED
    }
}
