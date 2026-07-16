package com.yxx.security.context;

import cn.dev33.satoken.stp.StpUtil;
import com.yxx.security.satoken.StpAdminUtil;
import org.springframework.stereotype.Component;

/** 使用 Sa-Token 用户端和管理端 API 执行实际 Redis 会话续期。 */
@Component
public class SaTokenSessionTimeoutRenewer implements SessionTimeoutRenewer {

    @Override
    public void renewUser(long timeoutSeconds) {
        StpUtil.renewTimeout(timeoutSeconds);
    }

    @Override
    public void renewAdmin(long timeoutSeconds) {
        StpAdminUtil.renewTimeout(timeoutSeconds);
    }
}
