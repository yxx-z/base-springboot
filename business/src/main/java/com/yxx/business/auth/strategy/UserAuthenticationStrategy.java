package com.yxx.business.auth.strategy;

import com.yxx.business.auth.command.UserAuthenticationCommand;
import com.yxx.business.auth.model.AuthenticatedUser;

/**
 * 用户认证策略。
 *
 * <p>策略只负责验证某一种身份并解析出系统用户；角色权限加载、Session 建立和 Token 返回由
 * 统一认证服务完成。</p>
 */
public interface UserAuthenticationStrategy {

    /** @return 当前策略支持的登录方式编码 */
    String loginMode();

    /**
     * 执行身份认证。
     *
     * @param command 登录命令
     * @return 认证成功的系统用户
     */
    AuthenticatedUser authenticate(UserAuthenticationCommand command);
}
