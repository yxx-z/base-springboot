package com.yxx.business.controller;

import com.yxx.business.model.request.AlipayLoginReq;
import com.yxx.business.auth.UserAuthenticationService;
import com.yxx.business.auth.command.AlipayAuthenticationCommand;
import com.yxx.business.model.response.LoginRes;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.framework.audit.model.AuditEventType;
import com.yxx.security.annotation.AllowAnonymous;
import com.yxx.security.constant.LoginDeviceType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付宝小程序鉴权
 *
 * @author yxx
 * @classname AliAuthController
 * @since 2023-09-14 11:13
 */
@Validated
@ResponseResult
@RestController
@RequestMapping("/aliAuth")
@RequiredArgsConstructor
public class AliAppletAuthController {

    private final UserAuthenticationService authenticationService;


    /**
     * 使用支付宝一次性授权码登录系统。
     *
     * <p>后端自行向支付宝换取平台用户编号，禁止直接信任前端提交的 userId。</p>
     *
     * @param req 支付宝授权码
     * @return 登录 Token
     */
    @AllowAnonymous
    @AuditLog(module = "鉴权模块", action = "支付宝授权登录",
            eventType = AuditEventType.AUTHENTICATION, recordRequest = false)
    @PostMapping("/login")
    public LoginRes login(@Valid @RequestBody AlipayLoginReq req) {
        String token = authenticationService.login(
                new AlipayAuthenticationCommand(req.authCode()), LoginDeviceType.ALIPAY_APPLET);
        return new LoginRes(token);
    }
}
