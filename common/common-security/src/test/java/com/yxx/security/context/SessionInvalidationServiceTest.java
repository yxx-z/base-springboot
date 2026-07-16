package com.yxx.security.context;

import com.yxx.security.properties.SessionInvalidationProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 会话失效事务顺序、去重和有限重试测试。 */
class SessionInvalidationServiceTest {

    @AfterEach
    void clearTransactionContext() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        // 测试异常中断时也清理自定义事务资源，避免同一测试线程污染后续用例。
        new ArrayList<>(TransactionSynchronizationManager.getResourceMap().keySet())
                .forEach(TransactionSynchronizationManager::unbindResourceIfPossible);
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldInvalidateImmediatelyWithoutTransaction() {
        TestFixture fixture = fixture(List.of(Duration.ofSeconds(1)));

        fixture.service().invalidateUserAfterCommit(
                10L, SessionInvalidationReason.PASSWORD_CHANGED);

        verify(fixture.loginSessionService()).invalidateUser(10L);
        verify(fixture.retryScheduler(), never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void shouldInvalidateOnlyAfterCommitAndDeduplicateSameSubject() {
        TestFixture fixture = fixture(List.of(Duration.ofSeconds(1)));
        beginTransactionSynchronization();

        fixture.service().invalidateAdminAfterCommit(
                20L, SessionInvalidationReason.SUBJECT_ROLE_CHANGED);
        fixture.service().invalidateAdminAfterCommit(
                20L, SessionInvalidationReason.ACCOUNT_DELETED);
        verify(fixture.loginSessionService(), never()).invalidateAdmin(20L);

        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        // 同一事务中的多条领域链路只对同一管理员执行一次 Redis 注销。
        verify(fixture.loginSessionService(), times(1)).invalidateAdmin(20L);
    }

    @Test
    void shouldNotInvalidateWhenTransactionRollsBack() {
        TestFixture fixture = fixture(List.of(Duration.ofSeconds(1)));
        beginTransactionSynchronization();
        fixture.service().invalidateUserAfterCommit(
                30L, SessionInvalidationReason.ACCOUNT_STATUS_CHANGED);

        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fixture.loginSessionService(), never()).invalidateUser(30L);
    }

    @Test
    void shouldRetryAsynchronouslyWithoutPropagatingRedisFailure() {
        TestFixture fixture = fixture(List.of(Duration.ofSeconds(1), Duration.ofSeconds(5)));
        doThrow(new IllegalStateException("redis unavailable"))
                .doNothing()
                .when(fixture.loginSessionService()).invalidateUser(40L);

        assertDoesNotThrow(() -> fixture.service().invalidateUserAfterCommit(
                40L, SessionInvalidationReason.PASSWORD_RESET));

        ArgumentCaptor<Runnable> retryTask = ArgumentCaptor.forClass(Runnable.class);
        verify(fixture.retryScheduler()).schedule(retryTask.capture(), any(Instant.class));
        retryTask.getValue().run();

        verify(fixture.loginSessionService(), times(2)).invalidateUser(40L);
        verify(fixture.eventPublisher(), never())
                .publishEvent(any(SessionInvalidationExhaustedEvent.class));
    }

    @Test
    void shouldPublishEventWhenAllRetriesAreExhausted() {
        TestFixture fixture = fixture(List.of(Duration.ofSeconds(1)));
        doThrow(new IllegalStateException("redis unavailable"))
                .when(fixture.loginSessionService()).invalidateAdmin(50L);

        fixture.service().invalidateAdminAfterCommit(
                50L, SessionInvalidationReason.PERMISSION_CHANGED);

        ArgumentCaptor<Runnable> retryTask = ArgumentCaptor.forClass(Runnable.class);
        verify(fixture.retryScheduler()).schedule(retryTask.capture(), any(Instant.class));
        retryTask.getValue().run();

        ArgumentCaptor<SessionInvalidationExhaustedEvent> event =
                ArgumentCaptor.forClass(SessionInvalidationExhaustedEvent.class);
        verify(fixture.eventPublisher()).publishEvent(event.capture());
        assertEquals("admin", event.getValue().realm());
        assertEquals(50L, event.getValue().subjectId());
        assertEquals(2, event.getValue().attemptCount());
    }

    private TestFixture fixture(List<Duration> retryDelays) {
        LoginSessionService loginSessionService = mock(LoginSessionService.class);
        TaskScheduler retryScheduler = mock(TaskScheduler.class);
        // 返回非空调度句柄表示任务已被正常受理，实际执行由用例捕获 Runnable 后控制。
        when(retryScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenReturn(mock(ScheduledFuture.class));
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        SessionInvalidationProperties properties = new SessionInvalidationProperties();
        properties.setRetryEnabled(true);
        properties.setRetryDelays(new ArrayList<>(retryDelays));
        SessionInvalidationService service = new SessionInvalidationService(
                loginSessionService, properties, retryScheduler, eventPublisher);
        return new TestFixture(service, loginSessionService, retryScheduler, eventPublisher);
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private void completeTransaction(int status) {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        if (status == TransactionSynchronization.STATUS_COMMITTED) {
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        }
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
    }

    private record TestFixture(
            SessionInvalidationService service,
            LoginSessionService loginSessionService,
            TaskScheduler retryScheduler,
            ApplicationEventPublisher eventPublisher) {
    }
}
