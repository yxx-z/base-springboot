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
        String reservation = RESERVATION_PREFIX + UUID.randomUUID();
        long result = redissonCache.reserveStringIfEquals(key, verificationCode, reservation);
        if (result < 0) {
            return ReservationResult.NOT_FOUND;
        }
        if (result == 0) {
            return ReservationResult.NOT_MATCHED;
        }
        registerCompletion(key, verificationCode, reservation);
        return ReservationResult.RESERVED;
    }

    private void registerCompletion(String key, String originalValue, String reservation) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            redissonCache.deleteStringIfEquals(key, reservation);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redissonCache.deleteStringIfEquals(key, reservation);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
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
