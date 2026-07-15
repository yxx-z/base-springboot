package com.yxx.business.auth.command;

import com.yxx.security.constant.LoginMode;

/**
 * 账号密码认证命令。
 *
 * @param account  登录账号
 * @param password 登录明文密码，仅在当前请求内使用
 */
public record PasswordAuthenticationCommand(String account, String password)
        implements UserAuthenticationCommand {

    @Override
    public String loginMode() {
        return LoginMode.PASSWORD;
    }
}
