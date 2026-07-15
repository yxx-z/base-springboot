package com.yxx.framework.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 安全事件监听器。
 *
 * <p>日志只记录账号体系、登录标识和事件类型，不记录 Token 原文。</p>
 */
@Slf4j
@Component
public class MySaTokenListener implements SaTokenListener {

    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginParameter loginParameter) {
        log.info("账号登录 loginType={} loginId={} device={}",
                loginType, loginId, loginParameter.getDeviceType());
    }

    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        log.info("账号注销 loginType={} loginId={}", loginType, loginId);
    }

    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        log.warn("账号被踢下线 loginType={} loginId={}", loginType, loginId);
    }

    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        log.warn("账号被顶下线 loginType={} loginId={}", loginType, loginId);
    }

    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        log.warn("账号被封禁 loginType={} loginId={} service={} level={} durationSeconds={}",
                loginType, loginId, service, level, disableTime);
    }

    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        log.info("账号解除封禁 loginType={} loginId={} service={}", loginType, loginId, service);
    }

    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
        log.info("开启二级认证 loginType={} service={} durationSeconds={}", loginType, service, safeTime);
    }

    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
        log.info("关闭二级认证 loginType={} service={}", loginType, service);
    }

    @Override
    public void doCreateSession(String id) {
        log.debug("创建 Sa-Token Session sessionId={}", id);
    }

    @Override
    public void doLogoutSession(String id) {
        log.debug("注销 Sa-Token Session sessionId={}", id);
    }

    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
        log.debug("Token 续期 loginId={} timeoutSeconds={}", loginId, timeout);
    }
}
