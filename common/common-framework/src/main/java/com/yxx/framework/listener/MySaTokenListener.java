package com.yxx.framework.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自定义侦听器的实现
 *
 * @author yxx
 * @since 2022/11/12 03:21
 */
@Slf4j
@Component
public class MySaTokenListener implements SaTokenListener {
    private static final String HALVING_LINE = "--------------------------------------------";
    /**
     * 每次登录时触发
     */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        log.info(HALVING_LINE);
        log.info("ID为:[{}]的用户, 正在登录[{}]端", loginId, loginModel.getDevice());
        log.info(HALVING_LINE);
    }

    /**
     * 每次注销时触发
     */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        log.info(HALVING_LINE);
        log.info("ID为:[{}]的用户, 正在注销登录", loginId);
        log.info(HALVING_LINE);
    }

    /**
     * 每次被踢下线时触发
     */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        log.info(HALVING_LINE);
        log.info("ID为:[{}]的用户, 被踢下线", loginId);
        log.info(HALVING_LINE);
    }

    /**
     * 每次被顶下线时触发
     */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        log.info(HALVING_LINE);
        log.info("ID为:[{}]的用户, 被顶下线,token值为[{}]", loginId, tokenValue);
        log.info(HALVING_LINE);
    }

    /**
     * 每次被封禁时触发
     */
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        long day = disableTime / 86400L;
        long hour = (disableTime % 86400) / 3600L;
        long minute = (disableTime % 86400) % 3600L / 60L;
        long second = (disableTime % 86400) % 3600L % 60L;
        log.info(HALVING_LINE);
        log.info("ID为:[{}]的用户, 被封禁账号,封禁时间为[{}]天[{}]小时[{}]分钟[{}]秒", loginId, day, hour, minute, second);
        log.info(HALVING_LINE);
    }

    /**
     * 每次被解封时触发
     */
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        log.info(HALVING_LINE);
        log.info("ID为:[{}]的用户, 已经解封", loginId);
        log.info(HALVING_LINE);
    }

    /** 每次二级认证时触发 */
    @Override
    public void doOpenSafe(String s, String s1, String s2, long l) {
        log.info("---------- 自定义侦听器实现 doOpenSafe");
    }

    /** 每次退出二级认证时触发 */
    @Override
    public void doCloseSafe(String s, String s1, String s2) {
        log.info("---------- 自定义侦听器实现 doOpenSafe");
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCreateSession(String id) {
        log.info("session被创建");
    }

    /**
     * 每次注销Session时触发
     */
    @Override
    public void doLogoutSession(String id) {
        log.info("session被注销");
    }

    /**
     * 每次Token续期时触发
     */
    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
        log.info(HALVING_LINE);
        log.info("ID为:[{}]的用户, token被续期,续期时间为[{}]秒", loginId, timeout);
        log.info(HALVING_LINE);
    }
}

