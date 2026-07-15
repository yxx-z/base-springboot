package com.yxx.security.context;

import cn.dev33.satoken.temp.SaTempUtil;
import com.yxx.common.utils.redis.RedissonCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

/**
 * Sa-Token 临时令牌的一次性消费协调器。
 *
 * <p>通过 Redis 原子占位防止同一个重置密码链接被并发提交。事务提交后才真正删除临时令牌；
 * 如果数据库事务回滚，则释放占位并保留原令牌，用户可以再次提交。</p>
 */
@Component
@RequiredArgsConstructor
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
        String reservationKey = RESERVATION_PREFIX + token;
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

        registerCompletion(token, reservationKey);
        return Optional.ofNullable(payload);
    }

    private void registerCompletion(String token, String reservationKey) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            SaTempUtil.deleteToken(token);
            redissonCache.remove(reservationKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                SaTempUtil.deleteToken(token);
            }

            @Override
            public void afterCompletion(int status) {
                // 无论事务提交还是回滚都清理短期占位；提交时原始令牌已经在 afterCommit 中销毁。
                redissonCache.remove(reservationKey);
            }
        });
    }
}
