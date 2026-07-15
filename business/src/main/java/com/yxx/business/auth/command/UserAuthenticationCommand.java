package com.yxx.business.auth.command;

/**
 * 用户端认证命令标记接口。
 *
 * <p>新增登录方式时创建新的命令类型和对应认证策略，不修改统一认证编排流程。</p>
 */
public interface UserAuthenticationCommand {

    /**
     * 返回该命令对应的登录方式编码。
     *
     * @return 登录方式
     */
    String loginMode();
}
