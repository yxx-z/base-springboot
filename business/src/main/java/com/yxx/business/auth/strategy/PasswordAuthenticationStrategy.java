package com.yxx.business.auth.strategy;

import com.yxx.business.auth.command.PasswordAuthenticationCommand;
import com.yxx.business.auth.command.UserAuthenticationCommand;
import com.yxx.business.auth.model.AuthenticatedUser;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.entity.UserIdentity;
import com.yxx.business.service.UserIdentityService;
import com.yxx.business.service.UserService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.utils.AccountNormalizer;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.ServletUtils;
import com.yxx.common.utils.ip.ClientIpResolver;
import com.yxx.security.constant.LoginMode;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.context.PasswordLoginProtectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 账号密码认证策略。
 */
@Component
@RequiredArgsConstructor
public class PasswordAuthenticationStrategy implements UserAuthenticationStrategy {

    private final UserIdentityService userIdentityService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordLoginProtectionService loginProtectionService;
    private final ClientIpResolver clientIpResolver;

    @Override
    public String loginMode() {
        return LoginMode.PASSWORD;
    }

    @Override
    public AuthenticatedUser authenticate(UserAuthenticationCommand command) {
        // 策略入口仍校验命令类型，避免错误路由后发生不明确的强制转换异常。
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, command instanceof PasswordAuthenticationCommand);
        PasswordAuthenticationCommand passwordCommand = (PasswordAuthenticationCommand) command;
        String account = AccountNormalizer.normalizeLoginCode(passwordCommand.account());
        // 客户端 IP 使用统一可信代理规则解析，不能直接信任任意 X-Forwarded-For。
        String clientIp = clientIpResolver.resolve(ServletUtils.getRequest());
        // 在昂贵的 BCrypt 比对之前预占频控额度，限制并发撞库对 CPU 的消耗。
        loginProtectionService.reserveAttempt(SecurityRealm.USER, account, clientIp);

        UserIdentity identity = userIdentityService
                .findAnyIdentity(LoginMode.PASSWORD, account)
                .orElse(null);
        if (identity == null
                || !passwordEncoder.matches(passwordCommand.password(), identity.getCredential())) {
            // 预占次数在认证失败时直接保留，统计窗口结束后由 Redis 自动清理。
            // 账号不存在和密码错误统一返回认证失败，避免账号枚举。
            throw new ApiException(ApiCode.AUTHENTICATION_FAILED);
        }

        // 只有凭据正确时才返回具体身份状态，避免攻击者通过错误密码枚举停用账号。
        loginProtectionService.recordSuccess(SecurityRealm.USER, account, clientIp);
        ApiAssert.isTrue(ApiCode.IDENTITY_DISABLED, Boolean.TRUE.equals(identity.getStatus()));
        ApiAssert.isTrue(ApiCode.IDENTITY_NOT_VERIFIED, Boolean.TRUE.equals(identity.getVerified()));

        User user = userService.findById(identity.getUserId());
        // 身份记录存在但主体缺失属于不一致数据，对外仍按认证失败处理。
        ApiAssert.isTrue(ApiCode.AUTHENTICATION_FAILED, user != null);
        ApiAssert.isTrue(ApiCode.ACCOUNT_DISABLED, Boolean.TRUE.equals(user.getStatus()));
        return new AuthenticatedUser(user, identity.getIdentifier(), loginMode());
    }
}
