package com.yxx.security.context;

import com.yxx.security.model.CurrentActor;
import com.yxx.security.model.LoginPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 基于 Sa-Token 会话的当前操作人提供者。
 */
@Component
@RequiredArgsConstructor
public class SaTokenCurrentActorProvider implements CurrentActorProvider {

    private final LoginSessionService loginSessionService;

    @Override
    public Optional<CurrentActor> currentActor() {
        // 两个应用独立部署，正常情况下只会命中一个账号体系；优先检查管理端可避免管理员
        // 会话被错误解释为普通用户会话。
        return loginSessionService.currentAdmin()
                .or(loginSessionService::currentUser)
                .map(this::toActor);
    }

    private CurrentActor toActor(LoginPrincipal principal) {
        return new CurrentActor(
                principal.getSubjectId(), principal.getSubjectType(),
                principal.getAccount(), principal.getDisplayName());
    }
}
