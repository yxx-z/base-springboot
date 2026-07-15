package com.yxx.admin.controller;

import com.yxx.admin.model.request.*;
import com.yxx.admin.service.AdminUserService;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.response.AdminCurrentUserRes;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.security.annotation.AllowAnonymous;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.model.LoginPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author yxx
 * @since 2022-11-12 02:07
 */
@Slf4j
@Validated
@ResponseResult
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    private final LoginSessionService loginSessionService;

    /**
     * 获取用户信息
     *
     * @return 当前管理员公开信息
     * @author yxx
     */
    @AuditLog(module = "用户模块", action = "获取当前管理员信息")
    @GetMapping("/info")
    public AdminCurrentUserRes info() {
        LoginPrincipal principal = loginSessionService.currentAdmin()
                .orElseThrow(() -> new com.yxx.common.exceptions.ApiException(
                        com.yxx.common.enums.ApiCode.TOKEN_ERROR));
        AdminUser user = adminUserService.getById(principal.getSubjectId());
        return new AdminCurrentUserRes(
                user.getId(), user.getLoginCode(), user.getLoginName(), user.getLinkPhone(),
                user.getEmail(), principal.getRoles(), principal.getPermissions());
    }

    /**
     * 发送重置密码邮件
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "发送重置密码邮件", recordRequest = false)
    @PostMapping("/resetPwdEmail")
    public Boolean resetPwdEmail(@Valid @RequestBody ResetPwdEmailReq req){
        return adminUserService.resetPwdEmail(req);
    }

    /**
     * 重置密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "重置密码", recordRequest = false)
    @PostMapping("/resetPwd")
    public Boolean resetPwd(@Valid @RequestBody ResetPwdReq req){
        return adminUserService.resetPwd(req);
    }

    /**
     * 修改密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @AuditLog(module = "用户模块", action = "修改密码", recordRequest = false)
    @PostMapping("/editPwd")
    public Boolean editPwd(@Valid @RequestBody EditPwdReq req){
        return adminUserService.editPwd(req);
    }

}
