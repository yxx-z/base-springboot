package com.yxx.business.controller;

import com.yxx.business.model.request.EditPwdReq;
import com.yxx.business.model.request.RegisterCaptchaReq;
import com.yxx.business.model.request.ResetPwdEmailReq;
import com.yxx.business.model.request.ResetPwdReq;
import com.yxx.business.model.request.UserRegisterReq;
import com.yxx.business.service.UserService;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.response.CurrentUserRes;
import com.yxx.rbac.model.RbacMenuNode;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.service.RbacRoleMenuService;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.framework.audit.model.AuditEventType;
import com.yxx.security.annotation.AllowAnonymous;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.model.LoginPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author yxx
 * @since 2022-11-12 02:07
 */
@Validated
@ResponseResult
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final LoginSessionService loginSessionService;

    private final RbacRoleMenuService roleMenuService;

    /**
     * 注册
     *
     * @param req 要求事情
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "用户注册", recordRequest = false,
            subjectField = "loginCode")
    @PostMapping("/register")
    public void register(@Valid @RequestBody UserRegisterReq req) {
        userService.register(req);
    }

    /**
     * 发送注册验证码
     *
     * @param req 要求事情
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "发送注册邮箱验证码", recordRequest = false)
    @PostMapping("/send-captcha")
    public void sendRegisterCaptcha(@Valid @RequestBody RegisterCaptchaReq req){
        userService.sendRegisterCaptcha(req);
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
        LoginPrincipal principal = loginSessionService.currentUser().orElseThrow(() -> new ApiException(ApiCode.TOKEN_ERROR));
        User user = userService.findById(principal.getSubjectId());
        if (user == null || !Boolean.TRUE.equals(user.getStatus())) {
            loginSessionService.invalidateUser(principal.getSubjectId());
            throw new ApiException(ApiCode.TOKEN_ERROR);
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
    public List<RbacMenuNode> menus() {
        LoginPrincipal principal = loginSessionService.currentUser()
                .orElseThrow(() -> new ApiException(ApiCode.TOKEN_ERROR));
        return roleMenuService.currentMenuTree(RbacScope.BUSINESS, principal.getRoles());
    }

    /**
     * 发送重置密码邮件
     *
     * @param req 要求事情
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "发送重置密码邮件",
            eventType = AuditEventType.SECURITY, recordRequest = false)
    @PostMapping("/reset-password-email")
    public void resetPwdEmail(@Valid @RequestBody ResetPwdEmailReq req){
        userService.resetPwdEmail(req);
    }

    /**
     * 重置密码
     *
     * @param req 要求事情
     * @author yxx
     */
    @AllowAnonymous
    @AuditLog(module = "用户模块", action = "重置密码",
            eventType = AuditEventType.SECURITY, recordRequest = false)
    @PostMapping("/reset-password")
    public void resetPwd(@Valid @RequestBody ResetPwdReq req){
        userService.resetPwd(req);
    }

    /**
     * 修改密码
     *
     * @param req 要求事情
     * @author yxx
     */
    @AuditLog(module = "用户模块", action = "修改密码",
            eventType = AuditEventType.SECURITY, recordRequest = false)
    @PostMapping("/change-password")
    public void editPwd(@Valid @RequestBody EditPwdReq req){
        userService.editPwd(req);
    }

}
