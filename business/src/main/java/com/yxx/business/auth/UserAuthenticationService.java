package com.yxx.business.auth;

import com.yxx.business.auth.command.UserAuthenticationCommand;
import com.yxx.business.auth.model.AuthenticatedUser;
import com.yxx.business.auth.model.AuthorizationSnapshot;
import com.yxx.business.auth.strategy.UserAuthenticationStrategy;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.model.LoginPrincipal;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端统一认证编排服务。
 *
 * <p>该服务采用策略模式：认证策略负责验证不同身份，编排服务统一负责授权快照加载、主体
 * 构造和 Sa-Token 会话建立。新增登录方式时只需增加策略实现。</p>
 */
@Service
public class UserAuthenticationService {

    private final Map<String, UserAuthenticationStrategy> strategies;
    private final UserAuthorizationService authorizationService;
    private final LoginSessionService loginSessionService;
    private final UserLoginRiskService loginRiskService;

    public UserAuthenticationService(List<UserAuthenticationStrategy> strategies,
                                     UserAuthorizationService authorizationService,
                                     LoginSessionService loginSessionService,
                                     UserLoginRiskService loginRiskService) {
        this.strategies = indexStrategies(strategies);
        this.authorizationService = authorizationService;
        this.loginSessionService = loginSessionService;
        this.loginRiskService = loginRiskService;
    }

    /**
     * 执行统一用户认证并建立会话。
     *
     * @param command 登录命令
     * @param device  登录设备
     * @return Sa-Token Token
     */
    public String login(UserAuthenticationCommand command, String device) {
        UserAuthenticationStrategy strategy = strategies.get(command.loginMode());
        if (strategy == null) {
            throw new ApiException(ApiCode.PARAM_IS_INVALID);
        }

        AuthenticatedUser authenticatedUser = strategy.authenticate(command);
        loginRiskService.handleSuccessfulLogin(authenticatedUser.user());
        AuthorizationSnapshot authorization = authorizationService.load(authenticatedUser.user().getId());
        LoginPrincipal principal = LoginPrincipal.builder()
                .subjectId(authenticatedUser.user().getId())
                .subjectType(SecurityRealm.USER)
                .account(authenticatedUser.account())
                .displayName(authenticatedUser.user().getDisplayName())
                .loginMode(authenticatedUser.loginMode())
                .roles(authorization.roles())
                .permissions(authorization.permissions())
                .loginTime(LocalDateTime.now())
                .build();
        return loginSessionService.loginUser(principal, device);
    }

    private Map<String, UserAuthenticationStrategy> indexStrategies(
            List<UserAuthenticationStrategy> strategyList) {
        Map<String, UserAuthenticationStrategy> result = new HashMap<>();
        for (UserAuthenticationStrategy strategy : strategyList) {
            UserAuthenticationStrategy duplicate = result.put(strategy.loginMode(), strategy);
            if (duplicate != null) {
                throw new IllegalStateException("存在重复登录策略：" + strategy.loginMode());
            }
        }
        return Map.copyOf(result);
    }
}
