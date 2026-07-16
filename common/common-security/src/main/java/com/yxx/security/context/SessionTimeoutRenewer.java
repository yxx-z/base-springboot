package com.yxx.security.context;

/** Sa-Token 静态 API 的可测试会话续期适配器。 */
public interface SessionTimeoutRenewer {

    void renewUser(long timeoutSeconds);

    void renewAdmin(long timeoutSeconds);
}
