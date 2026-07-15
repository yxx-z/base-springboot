package com.yxx.business.controller;

import com.yxx.business.model.request.*;
import com.yxx.business.service.UserService;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.response.CurrentUserRes;
import com.yxx.business.model.response.MenuRes;
import com.yxx.business.service.RoleMenuService;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.framework.audit.model.AuditEventType;
import com.yxx.security.annotation.AllowAnonymous;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.model.LoginPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
public class UserController {

    private final UserService userService;

    private final LoginSessionService loginSessionService;

    private final RoleMenuService roleMenuService;

    /**
     * 注册
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "用户注册", recordRequest = false)
    @PostMapping("/register")
    public Boolean register(@Valid @RequestBody UserRegisterReq req) {
        return userService.register(req);
    }

    /**
     * 发送注册验证码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "发送注册邮箱验证码", recordRequest = false)
    @PostMapping("/sendCaptcha")
    public Boolean sendRegisterCaptcha(@Valid @RequestBody RegisterCaptchaReq req){
        return userService.sendRegisterCaptcha(req);
    }

    /**
     * 获取用户信息
     *
     * @return 当前用户公开信息
     * @author yxx
     */
    @AuditLog(module = "用户模块", action = "获取当前用户信息")
    @GetMapping("/info")
    public CurrentUserRes info() {
        LoginPrincipal principal = loginSessionService.currentUser()
                .orElseThrow(() -> new com.yxx.common.exceptions.ApiException(
                        com.yxx.common.enums.ApiCode.TOKEN_ERROR));
        User user = userService.getById(principal.getSubjectId());
        if (user == null || !Boolean.TRUE.equals(user.getStatus())) {
            loginSessionService.invalidateUser(principal.getSubjectId());
            throw new com.yxx.common.exceptions.ApiException(com.yxx.common.enums.ApiCode.TOKEN_ERROR);
        }
        return new CurrentUserRes(
                user.getId(), principal.getAccount(), user.getDisplayName(), user.getAvatar(),
                user.getPhone(), user.getEmail(), principal.getRoles(), principal.getPermissions());
    }

    /**
     * 获取当前用户可见的导航菜单树。
     *
     * @return 当前角色关联的有效菜单
     */
    @GetMapping("/menus")
    public List<MenuRes> menus() {
        LoginPrincipal principal = loginSessionService.currentUser()
                .orElseThrow(() -> new com.yxx.common.exceptions.ApiException(
                        com.yxx.common.enums.ApiCode.TOKEN_ERROR));
        return roleMenuService.currentMenuTree(principal.getRoles());
    }

    /**
     * 发送重置密码邮件
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "发送重置密码邮件",
            eventType = AuditEventType.SECURITY, recordRequest = false)
    @PostMapping("/resetPwdEmail")
    public Boolean resetPwdEmail(@Valid @RequestBody ResetPwdEmailReq req){
        return userService.resetPwdEmail(req);
    }

    /**
     * 重置密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "重置密码",
            eventType = AuditEventType.SECURITY, recordRequest = false)
    @PostMapping("/resetPwd")
    public Boolean resetPwd(@Valid @RequestBody ResetPwdReq req){
        return userService.resetPwd(req);
    }

    /**
     * 修改密码
     *
     * @param req 要求事情
     * @return {@link Boolean }
     * @author yxx
     */
    @AuditLog(module = "用户模块", action = "修改密码",
            eventType = AuditEventType.SECURITY, recordRequest = false)
    @PostMapping("/editPwd")
    public Boolean editPwd(@Valid @RequestBody EditPwdReq req){
        return userService.editPwd(req);
    }

}
