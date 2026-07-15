package com.yxx.common.utils.satoken;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;

/**
 * 管理端 Sa-Token 多账号工具。
 *
 * <p>这里只暴露项目实际使用的最小 API，避免复制 Sa-Token 官方工具类的全部源码。
 * 依赖升级时由官方 {@link StpLogic} 保证 API 兼容，减少本地副本长期失修的问题。</p>
 */
public final class StpAdminUtil {

    /** 管理端账号体系标识。 */
    public static final String TYPE = "admin";

    private static final StpLogic STP_LOGIC = new StpLogic(TYPE);

    static {
        SaManager.putStpLogic(STP_LOGIC);
    }

    private StpAdminUtil() {
    }

    public static void login(Object loginId) {
        STP_LOGIC.login(loginId);
    }

    public static void login(Object loginId, String device) {
        STP_LOGIC.login(loginId, device);
    }

    public static void logout() {
        STP_LOGIC.logout();
    }

    public static boolean isLogin() {
        return STP_LOGIC.isLogin();
    }

    public static void checkLogin() {
        STP_LOGIC.checkLogin();
    }

    public static String getTokenValue() {
        return STP_LOGIC.getTokenValue();
    }

    public static SaSession getTokenSession() {
        return STP_LOGIC.getTokenSession();
    }
}
