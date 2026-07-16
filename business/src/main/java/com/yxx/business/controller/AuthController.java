package com.yxx.business.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yxx.business.auth.UserAuthenticationService;
import com.yxx.business.auth.command.PasswordAuthenticationCommand;
import com.yxx.business.model.request.LoginReq;
import com.yxx.business.model.response.LoginRes;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.framework.audit.model.AuditEventType;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.security.annotation.AllowAnonymous;
import com.yxx.security.constant.LoginDeviceType;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * @author yxx
 * @since 2023-05-17 10:02
 */
@Validated
@ResponseResult
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserAuthenticationService authenticationService;

    /**
     * 登录
     *
     * @param request 请求
     * @return {@link LoginRes }
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "鉴权模块", action = "用户密码登录",
            eventType = AuditEventType.AUTHENTICATION, recordRequest = false,
            subjectField = "loginCode")
    @PostMapping("/login")
    public LoginRes login(@Valid @RequestBody LoginReq request) {
        String token = authenticationService.login(
                new PasswordAuthenticationCommand(request.getLoginCode(), request.getPassword()),
                LoginDeviceType.PC);
        return new LoginRes(token);
    }

    /**
     * 注销
     *
     * @author yxx
     */
    @AuditLog(module = "鉴权模块", action = "用户退出", eventType = AuditEventType.AUTHENTICATION)
    @PostMapping("/logout")
    public void logout() {
        StpUtil.logout();
    }
}
