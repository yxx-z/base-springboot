package com.yxx.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 会话失效与事务提交顺序测试。 */
class SessionInvalidationServiceTest {

    @AfterEach
    void clearTransactionContext() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldInvalidateImmediatelyWithoutTransaction() {
        LoginSessionService loginSessionService = mock(LoginSessionService.class);
        SessionInvalidationService service = new SessionInvalidationService(loginSessionService);

        service.invalidateUserAfterCommit(10L);

        verify(loginSessionService).invalidateUser(10L);
    }

    @Test
    void shouldInvalidateOnlyAfterTransactionCommit() {
        LoginSessionService loginSessionService = mock(LoginSessionService.class);
        SessionInvalidationService service = new SessionInvalidationService(loginSessionService);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        service.invalidateAdminAfterCommit(20L);
        verify(loginSessionService, never()).invalidateAdmin(20L);

        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(loginSessionService).invalidateAdmin(20L);
    }
}
