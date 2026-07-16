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
        // 使用定向更新避免异步任务持有的旧实体覆盖管理员刚刚修改的其他资料。
        return update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, userId)
                .set(AdminUser::getAgent, agent)
                .set(AdminUser::getIpHomePlace, ipHomePlace));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long userId, boolean enabled) {
        // 读取当前记录既用于存在性校验，也为超级管理员保护逻辑提供明确主体。
        AdminUser user = findById(userId);
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, user != null);
        if (!enabled) {
            // 启用操作不会降低系统管理能力；停用前必须确保仍有其他可用超级管理员。
            assertNotLastActiveSuperAdmin(userId);
        }
        boolean updated = update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, userId)
                .set(AdminUser::getStatus, enabled));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        if (!enabled) {
            // 状态提交后再注销全部管理端会话，回滚时不影响仍然有效的管理员。
            sessionInvalidationService.invalidateAdminAfterCommit(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        AdminUser user = findById(userId);
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, user != null);
        // 无论目标是否当前为超级管理员，都统一经过保护检查以保持删除规则单一。
        assertNotLastActiveSuperAdmin(userId);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, removeById(userId));
        sessionInvalidationService.invalidateAdminAfterCommit(userId);
    }

    private void assertNotLastActiveSuperAdmin(Long userId) {
        // 先判断目标是否拥有内置超级角色，普通管理员无需执行全局活跃人数统计。
        boolean isSuperAdmin = baseMapper.countUserRole(userId, AdminSecurityCodes.ROLE_SUPER_ADMIN) > 0;
        if (isSuperAdmin) {
            // 必须至少保留另一名启用的超级管理员，避免管理端永久失去最高权限入口。
            ApiAssert.isTrue(ApiCode.LAST_SUPER_ADMIN,
                    baseMapper.countActiveUsersByRoleCode(AdminSecurityCodes.ROLE_SUPER_ADMIN) > 1);
        }
    }

    @Override
    public LoginRes login(LoginReq request) {
        // 请求上下文只读取一次，确保频控、风险识别和元数据记录使用同一来源信息。
        HttpServletRequest servletRequest = ServletUtils.getRequest();
        String requestIp = clientIpResolver.resolve(servletRequest);
        String loginCode = AccountNormalizer.normalizeLoginCode(request.getLoginCode());
        // BCrypt 比对前预占账号与 IP 双维度额度，避免并发请求穿透限流并消耗 CPU。
        loginProtectionService.reserveAttempt(SecurityRealm.ADMIN, loginCode, requestIp);

        // 账号不存在与密码错误统一响应，防止枚举管理员账号。
        AdminUser user = getOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getLoginCode, loginCode));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 预占次数在认证失败时直接保留，统计窗口结束后由 Redis 自动清理。
            throw new ApiException(ApiCode.AUTHENTICATION_FAILED);
        }
        loginProtectionService.recordSuccess(SecurityRealm.ADMIN, loginCode, requestIp);
        // 只有密码正确后才暴露停用状态，降低账户状态探测风险。
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
        // 风险通知和登录元数据属于辅助链路，交给有界线程池执行，不能拖慢或破坏登录结果。
        CompletableFuture.runAsync(
                        () -> updateMetadataAndCheckRisk(user, requestIp, agent),
                        applicationTaskExecutor)
                .exceptionally(exception -> {
                    log.error("处理管理员登录风险信息失败，adminId={}", user.getId(), exception);
                    return null;
                });

        // 客户端只需要令牌；角色权限保存在服务端账号 Session 中。
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
                // 管理端只接受 admin 安全域签发的令牌，防止用户端令牌跨域使用。
                .filter(value -> SecurityRealm.ADMIN.equals(value.realm()))
                .orElse(null);
        ApiAssert.isTrue(ApiCode.RESET_PWD_TOKEN_ERROR, payload != null);
        AdminUser user = getById(payload.subjectId());
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST,
                user != null && payload.email().equals(user.getEmail()));

        // 使用统一 BCrypt 编码器生成新密码。
        String password = passwordEncoder.encode(req.getNewPassword());
        // 通过令牌绑定的稳定内部 ID 定向更新，邮箱仅作为令牌与当前主体的一致性校验。
        boolean updated = update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, user.getId())
                .set(AdminUser::getPassword, password));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);

        // 新密码提交成功后注销全部旧会话，避免遗失 Token 继续访问管理端。
        sessionInvalidationService.invalidateAdminAfterCommit(user.getId());
    }


    @Override
    public AdminUser getUserByEmail(String email) {
        // 服务层统一规范化邮箱，避免调用方遗漏大小写和首尾空格处理。
        return getOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getEmail, AccountNormalizer.normalizeEmail(email)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPwd(EditPwdReq req) {
        // 修改目标只能来自当前管理端会话，不允许请求参数指定其他管理员 ID。
        Long userId = loginSessionService.currentAdmin()
                .map(LoginPrincipal::getSubjectId)
                .orElseThrow(() -> new ApiException(ApiCode.TOKEN_ERROR));
        AdminUser user = getById(userId);

        // PasswordEncoder.matches 的参数顺序为“请求明文、数据库摘要”，不能直接比较字符串。
        ApiAssert.isTrue(ApiCode.ORIGINAL_PASSWORD_ERROR, passwordEncoder.matches(req.getPassword(), user.getPassword()));

        // 新密码继续使用统一 BCrypt 策略保存。
        String newPassword = passwordEncoder.encode(req.getNewPassword());
        // 定向更新密码字段，避免覆盖并发修改的管理员名称、邮箱或状态。
        boolean updated = update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, user.getId())
                .set(AdminUser::getPassword, newPassword));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        sessionInvalidationService.invalidateAdminAfterCommit(userId);
    }

    private void updateMetadataAndCheckRisk(AdminUser user, String requestIp, String agent) {
        // 风险服务先基于“旧登录信息与本次信息”判断异常，再返回本次应持久化的归属地。
        String ipHomePlace = loginRiskNotificationService.process(
                SecurityRealm.ADMIN, user.getId(), user.getEmail(),
                user.getAgent(), user.getIpHomePlace(), requestIp, agent);
        boolean updated = updateLoginMetadata(user.getId(), agent, ipHomePlace);
        if (!updated) {
            log.warn("管理员登录元数据未更新，adminId={}", user.getId());
        }
    }
}
