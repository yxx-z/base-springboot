package com.yxx.security.context;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 登录会话失效协调器。
 *
 * <p>数据库事务存在时，会话必须在事务成功提交后再注销。这样既不会在事务回滚时误踢用户，
 * 也可以避免用户在事务提交前重新登录并再次加载旧权限或旧凭据。非事务调用则立即执行。</p>
 */
@Component
@RequiredArgsConstructor
public class SessionInvalidationService {

    private final LoginSessionService loginSessionService;

    /** 在当前事务提交后注销用户端全部会话。 */
    public void invalidateUserAfterCommit(Long subjectId) {
        executeAfterCommit(() -> loginSessionService.invalidateUser(subjectId));
    }

    /** 在当前事务提交后注销管理端全部会话。 */
    public void invalidateAdminAfterCommit(Long subjectId) {
        executeAfterCommit(() -> loginSessionService.invalidateAdmin(subjectId));
    }

    private void executeAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            // 非事务调用没有待提交状态，立即失效会话即可。
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 只监听提交成功；回滚时数据库状态未变化，不应误注销合法会话。
                action.run();
            }
        });
    }
}
