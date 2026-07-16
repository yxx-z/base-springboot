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
        // 先由 Sa-Token 建立 loginId 与 Token 的关联，此后才能取得对应的账号 Session。
        StpUtil.login(principal.getSubjectId(), device);
        // 主体快照存入 loginId Session，使同一账号的多个 Token 共用一致的授权信息。
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
        // 管理端使用独立 StpLogic，避免管理员身份与普通用户身份在同一安全域内串用。
        StpAdminUtil.login(principal.getSubjectId(), device);
        // 授权快照以稳定数据库 ID 为归属，不依赖可变的账号、手机号等登录凭证。
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
            // 未登录时不触发 getLoginId 异常，由上层统一决定匿名访问或拒绝访问。
            return Optional.empty();
        }
        // 从账号 Session 读取完整主体；不根据零散 Session 字段拼装残缺身份。
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
            // 管理端必须通过独立登录状态判断，不能复用用户端 StpUtil。
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
        if (SecurityRealm.USER.equals(loginType)) {
            return currentUser();
        }
        // 未知安全域不允许读取任何主体，避免拼写错误被静默解释为普通用户域。
        return Optional.empty();
    }

    /**
     * 根据 Sa-Token 回调提供的账号体系和登录标识读取授权主体。
     *
     * @param loginType Sa-Token 登录类型
     * @param loginId   稳定内部主体标识
     * @return 登录主体
     */
    public Optional<LoginPrincipal> findByLoginId(String loginType, Object loginId) {
        // 权限回调可能不处于 HTTP 请求中，因此直接按 loginId 查询账号 Session。
        Object value;
        if (SecurityRealm.ADMIN.equals(loginType)) {
            value = StpAdminUtil.getSessionByLoginId(loginId).get(PRINCIPAL_KEY);
        } else if (SecurityRealm.USER.equals(loginType)) {
            value = StpUtil.getSessionByLoginId(loginId).get(PRINCIPAL_KEY);
        } else {
            // 未知 loginType 按无授权主体处理，不访问错误的 Session 空间。
            return Optional.empty();
        }
        return principalFromSession(value);
    }

    /**
     * 角色或权限变更后注销指定用户的全部会话，使旧权限快照立即失效。
     *
     * @param subjectId 用户内部标识
     */
    public void invalidateUser(Long subjectId) {
        // logout(loginId) 注销该主体全部 Token，保证旧权限快照不能继续使用。
        StpUtil.logout(subjectId);
    }

    /**
     * 角色或权限变更后注销指定管理员的全部会话。
     *
     * @param subjectId 管理员内部标识
     */
    public void invalidateAdmin(Long subjectId) {
        // 管理端会话与用户端隔离，必须调用对应的 StpLogic 才能完成失效。
        StpAdminUtil.logout(subjectId);
    }

    private Optional<LoginPrincipal> principalFromSession(Object value) {
        // Session 丢失、反序列化类型不符时视为无有效主体，拒绝构造不可信的默认权限。
        return value instanceof LoginPrincipal principal ? Optional.of(principal) : Optional.empty();
    }
}
