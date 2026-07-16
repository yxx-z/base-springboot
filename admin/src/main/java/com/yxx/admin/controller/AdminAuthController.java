package com.yxx.admin.controller;

import com.yxx.admin.model.request.LoginReq;
import com.yxx.admin.model.response.LoginRes;
import com.yxx.admin.service.AdminUserService;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.framework.audit.model.AuditEventType;
import com.yxx.security.annotation.AllowAnonymous;
import com.yxx.security.satoken.StpAdminUtil;
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
public class AdminAuthController {
    private final AdminUserService adminUserService;

    /**
     * 登录
     *
     * @param request 请求
     * @return {@link LoginRes }
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "鉴权模块", action = "管理员密码登录",
            eventType = AuditEventType.AUTHENTICATION, recordRequest = false,
            subjectAccount = "#request.loginCode")
    @PostMapping("/login")
    public LoginRes login(@Valid @RequestBody LoginReq request) {
        return adminUserService.login(request);
    }

    /**
     * 注销
     *
     * @author yxx
     */
    @AuditLog(module = "鉴权模块", action = "管理员退出", eventType = AuditEventType.AUTHENTICATION)
    @PostMapping("/logout")
    public void logout() {
        StpAdminUtil.logout();
    }
}
