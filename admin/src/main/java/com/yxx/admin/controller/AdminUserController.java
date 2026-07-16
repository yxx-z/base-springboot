package com.yxx.admin.controller;

import com.yxx.admin.model.request.EditPwdReq;
import com.yxx.admin.model.request.ResetPwdEmailReq;
import com.yxx.admin.model.request.ResetPwdReq;
import com.yxx.admin.service.AdminUserService;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.response.AdminCurrentUserRes;
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
public class AdminUserController {

    private final AdminUserService adminUserService;

    private final LoginSessionService loginSessionService;

    private final RbacRoleMenuService roleMenuService;

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
                .orElseThrow(() -> new ApiException(ApiCode.TOKEN_ERROR));
        AdminUser user = adminUserService.findById(principal.getSubjectId());
        if (user == null || !Boolean.TRUE.equals(user.getStatus())) {
            loginSessionService.invalidateAdmin(principal.getSubjectId());
            throw new ApiException(ApiCode.TOKEN_ERROR);
        }
        return new AdminCurrentUserRes(
                user.getId(), user.getLoginCode(), user.getLoginName(), user.getLinkPhone(),
                user.getEmail(), principal.getRoles(), principal.getPermissions());
    }

    /** 获取当前管理员可见的导航菜单树。 */
    @GetMapping("/menus")
    public List<RbacMenuNode> menus() {
        LoginPrincipal principal = loginSessionService.currentAdmin()
                .orElseThrow(() -> new ApiException(ApiCode.TOKEN_ERROR));
        return roleMenuService.currentMenuTree(RbacScope.ADMIN, principal.getRoles());
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
        adminUserService.resetPwdEmail(req);
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
        adminUserService.resetPwd(req);
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
        adminUserService.editPwd(req);
    }

}
