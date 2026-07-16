package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminUserMapper;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.request.EditPwdReq;
import com.yxx.admin.model.request.LoginReq;
import com.yxx.admin.model.request.ResetPwdEmailReq;
import com.yxx.admin.model.request.ResetPwdReq;
import com.yxx.admin.model.response.LoginRes;
import com.yxx.admin.service.AdminUserService;
import com.yxx.admin.security.AdminAuthorizationService;
import com.yxx.admin.security.AdminSecurityCodes;
import com.yxx.security.constant.LoginDeviceType;
import com.yxx.common.constant.RedisKeyPrefix;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.AccountNormalizer;
import com.yxx.common.utils.ServletUtils;
import com.yxx.common.utils.agent.UserAgentUtil;
import com.yxx.common.utils.ip.ClientIpResolver;
import com.yxx.framework.security.PasswordResetMailService;
import com.yxx.framework.security.LoginRiskNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import com.yxx.security.constant.LoginMode;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.context.OneTimeTemporaryTokenService;
import com.yxx.security.context.PasswordLoginProtectionService;
import com.yxx.security.context.SessionInvalidationService;
import com.yxx.security.model.LoginPrincipal;
import com.yxx.security.model.PasswordResetTokenPayload;

/**
 * @author yxx
 * @since 2022-11-12 13:54
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {
    private final AdminAuthorizationService authorizationService;

    private final LoginSessionService loginSessionService;

    private final SessionInvalidationService sessionInvalidationService;

    private final OneTimeTemporaryTokenService oneTimeTemporaryTokenService;

    private final PasswordLoginProtectionService loginProtectionService;

    private final PasswordResetMailService passwordResetMailService;

    private final LoginRiskNotificationService loginRiskNotificationService;

    private final ClientIpResolver clientIpResolver;

    /**
     * 管理端与用户端共享统一密码编码策略，避免不同入口产生不兼容的密码格式。
     */
    private final PasswordEncoder passwordEncoder;

    /** 安全通知等非核心链路任务统一使用的有界线程池。 */
    @Qualifier("applicationTaskExecutor")
    private final Executor applicationTaskExecutor;

    @Override
    public AdminUser findById(Long userId) {
        return getById(userId);
    }

    @Override
    public boolean updateLoginMetadata(Long userId, String agent, String ipHomePlace) {
        return update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, userId)
                .set(AdminUser::getAgent, agent)
                .set(AdminUser::getIpHomePlace, ipHomePlace));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long userId, boolean enabled) {
        AdminUser user = findById(userId);
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, user != null);
        if (!enabled) {
            assertNotLastActiveSuperAdmin(userId);
        }
        boolean updated = update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, userId)
                .set(AdminUser::getStatus, enabled));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        if (!enabled) {
            sessionInvalidationService.invalidateAdminAfterCommit(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        AdminUser user = findById(userId);
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, user != null);
        assertNotLastActiveSuperAdmin(userId);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, removeById(userId));
        sessionInvalidationService.invalidateAdminAfterCommit(userId);
    }

    private void assertNotLastActiveSuperAdmin(Long userId) {
        boolean isSuperAdmin = baseMapper.countUserRole(userId, AdminSecurityCodes.ROLE_SUPER_ADMIN) > 0;
        if (isSuperAdmin) {
            ApiAssert.isTrue(ApiCode.LAST_SUPER_ADMIN,
                    baseMapper.countActiveUsersByRoleCode(AdminSecurityCodes.ROLE_SUPER_ADMIN) > 1);
        }
    }

    @Override
    public LoginRes login(LoginReq request) {
        HttpServletRequest servletRequest = ServletUtils.getRequest();
        String requestIp = clientIpResolver.resolve(servletRequest);
        String loginCode = AccountNormalizer.normalizeLoginCode(request.getLoginCode());
        loginProtectionService.reserveAttempt(SecurityRealm.ADMIN, loginCode, requestIp);

        // 根据登录账号获取用户信息
        AdminUser user = getOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getLoginCode, loginCode));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 预占次数在认证失败时直接保留，统计窗口结束后由 Redis 自动清理。
            throw new ApiException(ApiCode.AUTHENTICATION_FAILED);
        }
        loginProtectionService.recordSuccess(SecurityRealm.ADMIN, loginCode, requestIp);
        ApiAssert.isTrue(ApiCode.ACCOUNT_DISABLED, Boolean.TRUE.equals(user.getStatus()));

        // 获取登录设备信息。元数据更新与异地登录提醒属于辅助链路，不阻断认证和会话创建。
        String requestAgent = servletRequest.getHeader("user-agent");
        String agent = UserAgentUtil.getAgent(requestAgent);

        // 角色与后端权限分别加载，菜单仅用于前端导航，不再作为接口权限使用。
        AdminAuthorizationService.Snapshot authorization = authorizationService.load(user.getId());
        LoginPrincipal principal = LoginPrincipal.builder()
                .subjectId(user.getId())
                .subjectType(SecurityRealm.ADMIN)
                .account(user.getLoginCode())
                .displayName(user.getLoginName())
                .loginMode(LoginMode.PASSWORD)
                .roles(authorization.roles())
                .permissions(authorization.permissions())
                .loginTime(LocalDateTime.now())
                .build();
        String token = loginSessionService.loginAdmin(principal, LoginDeviceType.PC);
        CompletableFuture.runAsync(
                        () -> updateMetadataAndCheckRisk(user, requestIp, agent),
                        applicationTaskExecutor)
                .exceptionally(exception -> {
                    log.error("处理管理员登录风险信息失败，adminId={}", user.getId(), exception);
                    return null;
                });

        // 返回token
        return new LoginRes(token);
    }

    @Override
    public void resetPwdEmail(ResetPwdEmailReq req) {
        String email = AccountNormalizer.normalizeEmail(req.getEmail());
        AdminUser user = getUserByEmail(email);
        if (user == null) {
            // 管理端同样隐藏账号存在性，防止外部枚举管理员邮箱。
            return;
        }
        passwordResetMailService.send(
                SecurityRealm.ADMIN, user.getId(), email,
                RedisKeyPrefix.ADMIN_RESET_PASSWORD_TOKEN,
                RedisKeyPrefix.ADMIN_RESET_PASSWORD_COUNT);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void resetPwd(ResetPwdReq req) {
        // 原子取得一次性令牌消费权，避免同一个重置链接被并发使用。
        PasswordResetTokenPayload payload = oneTimeTemporaryTokenService
                .reserve(req.getToken(), PasswordResetTokenPayload::decode)
                .filter(value -> SecurityRealm.ADMIN.equals(value.realm()))
                .orElse(null);
        ApiAssert.isTrue(ApiCode.RESET_PWD_TOKEN_ERROR, payload != null);
        AdminUser user = getById(payload.subjectId());
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST,
                user != null && payload.email().equals(user.getEmail()));

        // 使用统一 BCrypt 编码器生成新密码。
        String password = passwordEncoder.encode(req.getNewPassword());
        // 根据邮箱修改密码
        boolean updated = update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, user.getId())
                .set(AdminUser::getPassword, password));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);

        sessionInvalidationService.invalidateAdminAfterCommit(user.getId());
    }


    @Override
    public AdminUser getUserByEmail(String email) {
        return getOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getEmail, AccountNormalizer.normalizeEmail(email)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPwd(EditPwdReq req) {
        // 根据登录id 获取该用户详情
        Long userId = loginSessionService.currentAdmin()
                .map(LoginPrincipal::getSubjectId)
                .orElseThrow(() -> new ApiException(ApiCode.TOKEN_ERROR));
        AdminUser user = getById(userId);

        // 匹对请求参数中的旧密码是否正确
        ApiAssert.isTrue(ApiCode.ORIGINAL_PASSWORD_ERROR, passwordEncoder.matches(req.getPassword(), user.getPassword()));

        // 新密码继续使用统一 BCrypt 策略保存。
        String newPassword = passwordEncoder.encode(req.getNewPassword());
        // 根据用户id修改新密码
        boolean updated = update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, user.getId())
                .set(AdminUser::getPassword, newPassword));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        sessionInvalidationService.invalidateAdminAfterCommit(userId);
    }

    private void updateMetadataAndCheckRisk(AdminUser user, String requestIp, String agent) {
        String ipHomePlace = loginRiskNotificationService.process(
                SecurityRealm.ADMIN, user.getId(), user.getEmail(),
                user.getAgent(), user.getIpHomePlace(), requestIp, agent);
        boolean updated = updateLoginMetadata(user.getId(), agent, ipHomePlace);
        if (!updated) {
            log.warn("管理员登录元数据未更新，adminId={}", user.getId());
        }
    }
}
