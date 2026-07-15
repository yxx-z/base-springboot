package com.yxx.business.auth.command;

import com.yxx.security.constant.LoginMode;

/**
 * 支付宝授权认证命令。
 *
 * @param authCode 支付宝客户端取得的一次性授权码
 */
public record AlipayAuthenticationCommand(String authCode) implements UserAuthenticationCommand {

    @Override
    public String loginMode() {
        return LoginMode.ALIPAY;
    }
}
