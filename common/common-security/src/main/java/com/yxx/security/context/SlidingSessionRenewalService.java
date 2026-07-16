package com.yxx.security.context;

import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.properties.SlidingSessionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 会话滑动续期协调器。
 *
 * <p>续期必须发生在登录校验成功之后，匿名请求和已经过期的 Token 不能获得新的有效期。
 * 该组件只续签当前请求对应的安全域，避免普通用户 Token 与管理员 Token 相互串用。</p>
 */
@Component
@RequiredArgsConstructor
public class SlidingSessionRenewalService {

    private final SlidingSessionProperties properties;
    private final SessionTimeoutRenewer timeoutRenewer;

    /** 对当前安全域的已认证会话续签。 */
    public void renewCurrentSession(String realm) {
        if (!properties.isEnabled()) {
            return;
        }
        long timeoutSeconds = properties.getTimeout().toSeconds();
        if (SecurityRealm.USER.equals(realm)) {
            timeoutRenewer.renewUser(timeoutSeconds);
            return;
        }
        if (SecurityRealm.ADMIN.equals(realm)) {
            timeoutRenewer.renewAdmin(timeoutSeconds);
            return;
        }
        throw new IllegalStateException("不支持的会话安全域：" + realm);
    }
}
