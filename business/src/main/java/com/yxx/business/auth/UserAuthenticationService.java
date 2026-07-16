package com.yxx.business.auth;

import com.yxx.business.auth.command.UserAuthenticationCommand;
import com.yxx.business.auth.model.AuthenticatedUser;
import com.yxx.business.auth.strategy.UserAuthenticationStrategy;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.authorization.AuthorizationProvider;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.model.AuthorizationSnapshot;
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
    private final AuthorizationProvider authorizationProvider;
    private final LoginSessionService loginSessionService;
    private final UserLoginRiskService loginRiskService;

    public UserAuthenticationService(List<UserAuthenticationStrategy> strategies,
                                     AuthorizationProvider authorizationProvider,
                                     LoginSessionService loginSessionService,
                                     UserLoginRiskService loginRiskService) {
        // 启动时将策略按登录模式建立不可变索引，登录请求无需遍历 Spring Bean 集合。
        this.strategies = indexStrategies(strategies);
        this.authorizationProvider = authorizationProvider;
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
        // 编排层只按模式路由，不包含某一种登录方式的凭据校验细节。
        UserAuthenticationStrategy strategy = strategies.get(command.loginMode());
        if (strategy == null) {
            throw new ApiException(ApiCode.PARAM_IS_INVALID);
        }

        // 认证策略返回统一用户后，后续授权和会话创建流程对所有登录方式一致。
        AuthenticatedUser authenticatedUser = strategy.authenticate(command);
        // 登录时生成权限快照；权限变更通过注销会话使快照立即失效。
        AuthorizationSnapshot authorization = authorizationProvider.load(
                SecurityRealm.USER, authenticatedUser.user().getId());
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
        String token = loginSessionService.loginUser(principal, device);
        // 只有授权快照加载和 Session 创建全部成功后，才记录成功登录元数据并发送风险提醒。
        loginRiskService.handleSuccessfulLogin(authenticatedUser.user());
        return token;
    }

    private Map<String, UserAuthenticationStrategy> indexStrategies(
            List<UserAuthenticationStrategy> strategyList) {
        Map<String, UserAuthenticationStrategy> result = new HashMap<>();
        for (UserAuthenticationStrategy strategy : strategyList) {
            // 登录模式必须全局唯一；静默覆盖会使实际启用的策略取决于 Bean 加载顺序。
            UserAuthenticationStrategy duplicate = result.put(strategy.loginMode(), strategy);
            if (duplicate != null) {
                throw new IllegalStateException("存在重复登录策略：" + strategy.loginMode());
            }
        }
        // 暴露不可变 Map，防止运行期误修改认证策略集合。
        return Map.copyOf(result);
    }
}
