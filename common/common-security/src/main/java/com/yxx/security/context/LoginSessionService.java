package com.yxx.security.context;

import cn.dev33.satoken.stp.StpUtil;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.model.LoginPrincipal;
import com.yxx.security.satoken.StpAdminUtil;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 统一登录会话服务。
 *
 * <p>业务模块只负责构造 {@link LoginPrincipal}，具体使用哪个 Sa-Token 账号体系、如何写入
 * Token-Session 由本服务统一处理，避免在业务代码中复制静态登录工具。</p>
 */
@Component
public class LoginSessionService {

    private static final String PRINCIPAL_KEY = "security:principal";

    /**
     * 建立用户端登录会话。
     *
     * @param principal 已完成认证和授权装配的用户主体
     * @param device    登录设备类型
     * @return 当前 Token
     */
    public String loginUser(LoginPrincipal principal, String device) {
        StpUtil.login(principal.getSubjectId(), device);
        StpUtil.getSessionByLoginId(principal.getSubjectId()).set(PRINCIPAL_KEY, principal);
        return StpUtil.getTokenValue();
    }

    /**
     * 建立管理端登录会话。
     *
     * @param principal 已完成认证和授权装配的管理员主体
     * @param device    登录设备类型
     * @return 当前 Token
     */
    public String loginAdmin(LoginPrincipal principal, String device) {
        StpAdminUtil.login(principal.getSubjectId(), device);
        StpAdminUtil.getSessionByLoginId(principal.getSubjectId()).set(PRINCIPAL_KEY, principal);
        return StpAdminUtil.getTokenValue();
    }

    /**
     * 获取当前用户端主体。
     *
     * @return 当前用户主体；未登录或 Session 不完整时返回空
     */
    public Optional<LoginPrincipal> currentUser() {
        if (!StpUtil.isLogin()) {
            return Optional.empty();
        }
        return principalFromSession(
                StpUtil.getSessionByLoginId(StpUtil.getLoginId()).get(PRINCIPAL_KEY));
    }

    /**
     * 获取当前管理端主体。
     *
     * @return 当前管理员主体；未登录或 Session 不完整时返回空
     */
    public Optional<LoginPrincipal> currentAdmin() {
        if (!StpAdminUtil.isLogin()) {
            return Optional.empty();
        }
        return principalFromSession(
                StpAdminUtil.getSessionByLoginId(StpAdminUtil.getLoginId()).get(PRINCIPAL_KEY));
    }

    /**
     * 根据 Sa-Token 账号体系读取当前主体。
     *
     * @param loginType Sa-Token 登录类型
     * @return 当前主体
     */
    public Optional<LoginPrincipal> currentByLoginType(String loginType) {
        if (SecurityRealm.ADMIN.equals(loginType)) {
            return currentAdmin();
        }
        return currentUser();
    }

    /**
     * 根据 Sa-Token 回调提供的账号体系和登录标识读取授权主体。
     *
     * @param loginType Sa-Token 登录类型
     * @param loginId   稳定内部主体标识
     * @return 登录主体
     */
    public Optional<LoginPrincipal> findByLoginId(String loginType, Object loginId) {
        Object value = SecurityRealm.ADMIN.equals(loginType)
                ? StpAdminUtil.getSessionByLoginId(loginId).get(PRINCIPAL_KEY)
                : StpUtil.getSessionByLoginId(loginId).get(PRINCIPAL_KEY);
        return principalFromSession(value);
    }

    /**
     * 角色或权限变更后注销指定用户的全部会话，使旧权限快照立即失效。
     *
     * @param subjectId 用户内部标识
     */
    public void invalidateUser(Long subjectId) {
        StpUtil.logout(subjectId);
    }

    /**
     * 角色或权限变更后注销指定管理员的全部会话。
     *
     * @param subjectId 管理员内部标识
     */
    public void invalidateAdmin(Long subjectId) {
        StpAdminUtil.logout(subjectId);
    }

    private Optional<LoginPrincipal> principalFromSession(Object value) {
        return value instanceof LoginPrincipal principal ? Optional.of(principal) : Optional.empty();
    }
}
