package com.yxx.business.auth.strategy;

import com.yxx.business.auth.command.PasswordAuthenticationCommand;
import com.yxx.business.auth.command.UserAuthenticationCommand;
import com.yxx.business.auth.model.AuthenticatedUser;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.entity.UserIdentity;
import com.yxx.business.service.UserIdentityService;
import com.yxx.business.service.UserService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.security.constant.LoginMode;
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

    @Override
    public String loginMode() {
        return LoginMode.PASSWORD;
    }

    @Override
    public AuthenticatedUser authenticate(UserAuthenticationCommand command) {
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, command instanceof PasswordAuthenticationCommand);
        PasswordAuthenticationCommand passwordCommand = (PasswordAuthenticationCommand) command;

        UserIdentity identity = userIdentityService
                .findIdentity(LoginMode.PASSWORD, passwordCommand.account())
                .orElse(null);
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, identity != null);
        ApiAssert.isTrue(ApiCode.PASSWORD_ERROR,
                passwordEncoder.matches(passwordCommand.password(), identity.getCredential()));

        User user = userService.getById(identity.getUserId());
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST,
                user != null && Boolean.TRUE.equals(user.getStatus()));
        return new AuthenticatedUser(user, identity.getIdentifier(), loginMode());
    }
}
