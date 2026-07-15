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
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, command instanceof PasswordAuthenticationCommand);
        PasswordAuthenticationCommand passwordCommand = (PasswordAuthenticationCommand) command;
        String clientIp = clientIpResolver.resolve(ServletUtils.getRequest());
        loginProtectionService.checkAllowed(SecurityRealm.USER, passwordCommand.account(), clientIp);

        UserIdentity identity = userIdentityService
                .findIdentity(LoginMode.PASSWORD, passwordCommand.account())
                .orElse(null);
        if (identity == null
                || !passwordEncoder.matches(passwordCommand.password(), identity.getCredential())) {
            loginProtectionService.recordFailure(SecurityRealm.USER, passwordCommand.account(), clientIp);
            throw new ApiException(ApiCode.AUTHENTICATION_FAILED);
        }

        User user = userService.getById(identity.getUserId());
        ApiAssert.isTrue(ApiCode.AUTHENTICATION_FAILED, user != null);
        ApiAssert.isTrue(ApiCode.ACCOUNT_DISABLED, Boolean.TRUE.equals(user.getStatus()));
        loginProtectionService.recordSuccess(SecurityRealm.USER, passwordCommand.account());
        return new AuthenticatedUser(user, identity.getIdentifier(), loginMode());
    }
}
