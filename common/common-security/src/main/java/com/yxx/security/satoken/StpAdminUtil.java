package com.yxx.security.satoken;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import com.yxx.security.constant.SecurityRealm;

/**
 * 管理端 Sa-Token 多账号工具。
 *
 * <p>管理端使用独立 {@link StpLogic}，确保管理员 Token、Session 和普通用户账号体系彼此隔离。</p>
 */
public final class StpAdminUtil {

    /** 管理端 Sa-Token 账号体系标识。 */
    public static final String TYPE = SecurityRealm.ADMIN;

    private static final StpLogic STP_LOGIC = new StpLogic(TYPE);

    static {
        // 在 Sa-Token 全局管理器中注册独立逻辑，保证框架回调可按 admin 类型定位。
        SaManager.putStpLogic(STP_LOGIC);
    }

    private StpAdminUtil() {
    }

    public static void login(Object loginId, String device) {
        // 所有管理端静态操作都委托给同一个独立 StpLogic，禁止混用默认 StpUtil。
        STP_LOGIC.login(loginId, device);
    }

    public static void logout() {
        STP_LOGIC.logout();
    }

    /**
     * 注销指定管理员的全部登录会话，用于角色或权限变更后立即失效权限快照。
     *
     * @param loginId 管理员内部稳定标识
     */
    public static void logout(Object loginId) {
        STP_LOGIC.logout(loginId);
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

    public static Object getLoginId() {
        return STP_LOGIC.getLoginId();
    }

    public static SaSession getSessionByLoginId(Object loginId) {
        return STP_LOGIC.getSessionByLoginId(loginId);
    }
}
