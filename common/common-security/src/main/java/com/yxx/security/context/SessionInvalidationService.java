package com.yxx.security.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.properties.SessionInvalidationProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

/**
 * 登录会话失效协调器。
 *
 * <p>数据库事务存在时，会话必须在事务成功提交后再注销。这样既不会在事务回滚时误踢用户，
 * 也可以避免用户在事务提交前重新登录并再次加载旧权限或旧凭据。非事务调用则立即执行。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionInvalidationService {

    private static final Object TRANSACTION_RESOURCE_KEY =
            SessionInvalidationService.class.getName() + ".requests";

    private final LoginSessionService loginSessionService;
    private final SessionInvalidationProperties properties;

    @Qualifier("sessionInvalidationTaskScheduler")
    private final TaskScheduler retryScheduler;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 在当前事务提交后注销用户端全部会话。
     *
     * @param subjectId 用户数据库内部标识
     * @param reason    会话失效原因
     */
    public void invalidateUserAfterCommit(Long subjectId, SessionInvalidationReason reason) {
        registerAfterCommit(SecurityRealm.USER, subjectId, reason);
    }

    /**
     * 在当前事务提交后注销管理端全部会话。
     *
     * @param subjectId 管理员数据库内部标识
     * @param reason    会话失效原因
     */
    public void invalidateAdminAfterCommit(Long subjectId, SessionInvalidationReason reason) {
        registerAfterCommit(SecurityRealm.ADMIN, subjectId, reason);
    }

    private void registerAfterCommit(
            String realm, Long subjectId, SessionInvalidationReason reason) {
        if (subjectId == null || reason == null) {
            throw new IllegalArgumentException("会话失效主体和原因不能为空");
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            // 非事务调用没有待提交状态，立即失效会话即可。
            invalidateSafely(new InvalidationRequest(realm, subjectId, EnumSet.of(reason)), 0);
            return;
        }

        /*
         * 同一个业务事务可能先撤销角色、再软删用户，两条领域链路都会要求注销同一账号。
         * 使用事务资源按“安全域 + 主体 ID”合并原因，只在提交后执行一次 Redis 注销。
         */
        InvalidationBatch batch = currentOrCreateBatch();
        batch.add(realm, subjectId, reason);
    }

    @SuppressWarnings("unchecked")
    private InvalidationBatch currentOrCreateBatch() {
        if (TransactionSynchronizationManager.hasResource(TRANSACTION_RESOURCE_KEY)) {
            return (InvalidationBatch) TransactionSynchronizationManager
                    .getResource(TRANSACTION_RESOURCE_KEY);
        }

        InvalidationBatch batch = new InvalidationBatch();
        TransactionSynchronizationManager.bindResource(TRANSACTION_RESOURCE_KEY, batch);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 数据库已经成功提交，逐主体立即尝试注销全部 Token。
                batch.requests().values().forEach(request -> invalidateSafely(request, 0));
            }

            @Override
            public void afterCompletion(int status) {
                // 无论提交还是回滚都清理线程事务资源，防止线程复用污染后续事务。
                if (TransactionSynchronizationManager.hasResource(TRANSACTION_RESOURCE_KEY)) {
                    TransactionSynchronizationManager.unbindResource(TRANSACTION_RESOURCE_KEY);
                }
            }
        });
        return batch;
    }

    private void invalidateSafely(InvalidationRequest request, int retryIndex) {
        int attempt = retryIndex + 1;
        try {
            invalidate(request);
            if (retryIndex > 0) {
                log.info("会话失效重试成功，realm={}，subjectId={}，reasons={}，attempt={}",
                        request.realm(), request.subjectId(), request.reasons(), attempt);
            }
        } catch (RuntimeException exception) {
            // 数据库已经提交，Redis 异常只能进入补偿链路，不能再向业务接口传播失败。
            scheduleNextRetry(request, retryIndex, exception);
        }
    }

    private void invalidate(InvalidationRequest request) {
        if (SecurityRealm.USER.equals(request.realm())) {
            loginSessionService.invalidateUser(request.subjectId());
            return;
        }
        if (SecurityRealm.ADMIN.equals(request.realm())) {
            loginSessionService.invalidateAdmin(request.subjectId());
            return;
        }
        throw new IllegalStateException("不支持的会话安全域：" + request.realm());
    }

    private void scheduleNextRetry(
            InvalidationRequest request, int retryIndex, RuntimeException exception) {
        if (!properties.isRetryEnabled() || retryIndex >= properties.getRetryDelays().size()) {
            reportExhausted(request, retryIndex + 1, exception);
            return;
        }

        Duration delay = properties.getRetryDelays().get(retryIndex);
        int nextRetryIndex = retryIndex + 1;
        log.warn("会话失效失败，已安排异步重试，realm={}，subjectId={}，reasons={}，"
                        + "attempt={}，nextDelay={}，errorType={}",
                request.realm(), request.subjectId(), request.reasons(), retryIndex + 1,
                delay, exception.getClass().getSimpleName());
        try {
            // 使用绝对执行时间交给独立调度线程池，禁止在业务线程中 sleep 阻塞。
            ScheduledFuture<?> scheduledFuture = retryScheduler.schedule(
                    () -> invalidateSafely(request, nextRetryIndex), Instant.now().plus(delay));
            if (scheduledFuture == null) {
                // 部分调度器在关闭过程中以 null 表示任务未受理，不能把本次重试静默丢失。
                IllegalStateException notScheduled =
                        new IllegalStateException("会话失效重试任务未被调度器接收");
                notScheduled.addSuppressed(exception);
                reportExhausted(request, retryIndex + 1, notScheduled);
            }
        } catch (RuntimeException schedulingException) {
            // 调度器关闭或队列拒绝时已无法继续内存重试，立即进入耗尽告警。
            schedulingException.addSuppressed(exception);
            reportExhausted(request, retryIndex + 1, schedulingException);
        }
    }

    private void reportExhausted(
            InvalidationRequest request, int attemptCount, RuntimeException exception) {
        log.error("会话失效重试已耗尽，realm={}，subjectId={}，reasons={}，attemptCount={}",
                request.realm(), request.subjectId(), request.reasons(), attemptCount, exception);
        // 发布标准 Spring 事件，具体项目可以无侵入地转换为 Micrometer 指标或外部告警。
        try {
            eventPublisher.publishEvent(new SessionInvalidationExhaustedEvent(
                    request.realm(), request.subjectId(), Set.copyOf(request.reasons()), attemptCount,
                    exception.getClass().getName(), exception.getMessage()));
        } catch (RuntimeException publishingException) {
            /*
             * Spring 事件默认同步发布，项目自定义的告警监听器也可能发生异常。数据库已
             * 提交且 Redis 注销已失败，此处只能记录，禁止告警链路再次影响业务调用。
             */
            log.error("发布会话失效重试耗尽事件失败，realm={}，subjectId={}",
                    request.realm(), request.subjectId(), publishingException);
        }
    }

    private record InvalidationKey(String realm, Long subjectId) {
    }

    private record InvalidationRequest(
            String realm, Long subjectId, EnumSet<SessionInvalidationReason> reasons) {
    }

    private static final class InvalidationBatch {

        private final Map<InvalidationKey, InvalidationRequest> requests = new LinkedHashMap<>();

        private void add(String realm, Long subjectId, SessionInvalidationReason reason) {
            InvalidationKey key = new InvalidationKey(realm, subjectId);
            requests.compute(key, (ignored, existing) -> {
                if (existing == null) {
                    return new InvalidationRequest(realm, subjectId, EnumSet.of(reason));
                }
                existing.reasons().add(reason);
                return existing;
            });
        }

        private Map<InvalidationKey, InvalidationRequest> requests() {
            return requests;
        }
    }
}
