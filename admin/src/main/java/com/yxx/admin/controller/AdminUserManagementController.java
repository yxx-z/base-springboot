package com.yxx.admin.controller;

import com.yxx.admin.model.request.AdminUserPageReq;
import com.yxx.admin.model.request.ChangeStatusReq;
import com.yxx.admin.model.request.CreateAdminUserReq;
import com.yxx.admin.model.request.ReplaceRoleIdsReq;
import com.yxx.admin.model.request.UpdateAdminUserReq;
import com.yxx.admin.model.response.ManagedAdminUserRes;
import com.yxx.admin.security.AdminSecurityCodes;
import com.yxx.admin.service.AdminUserManagementService;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.core.page.PageResponse;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.security.annotation.SaAdminCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员账号完整管理接口。 */
@Validated
@ResponseResult
@RestController
@RequestMapping("/management/admin-users")
@RequiredArgsConstructor
public class AdminUserManagementController {

    private final AdminUserManagementService managementService;

    @GetMapping
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_ADMIN_USER_READ)
    public PageResponse<ManagedAdminUserRes> page(@Valid AdminUserPageReq request) {
        return managementService.page(request);
    }

    @GetMapping("/{userId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_ADMIN_USER_READ)
    public ManagedAdminUserRes detail(@PathVariable @Positive Long userId) {
        return managementService.findById(userId);
    }

    @PostMapping
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_ADMIN_USER_WRITE)
    @AuditLog(module = "管理员管理", action = "新增管理员", resource = "admin-user",
            subjectType = "admin-user", subjectAccount = "#request.loginCode")
    public Long create(@Valid @RequestBody CreateAdminUserReq request) {
        return managementService.create(request);
    }

    @PutMapping("/{userId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_ADMIN_USER_WRITE)
    @AuditLog(module = "管理员管理", action = "修改管理员", resource = "admin-user",
            subjectType = "admin-user", subjectId = "#userId")
    public void update(@PathVariable @Positive Long userId,
                       @Valid @RequestBody UpdateAdminUserReq request) {
        managementService.update(userId, request);
    }

    @PutMapping("/{userId}/status")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_ADMIN_USER_WRITE)
    @AuditLog(module = "管理员管理", action = "修改管理员状态", resource = "admin-user",
            subjectType = "admin-user", subjectId = "#userId")
    public void changeStatus(@PathVariable @Positive Long userId,
                             @Valid @RequestBody ChangeStatusReq request) {
        managementService.changeStatus(userId, request.enabled());
    }

    @PutMapping("/{userId}/roles")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_ADMIN_USER_WRITE)
    @AuditLog(module = "管理员管理", action = "配置管理员角色", resource = "admin-user",
            subjectType = "admin-user", subjectId = "#userId")
    public void replaceRoles(@PathVariable @Positive Long userId,
                             @Valid @RequestBody ReplaceRoleIdsReq request) {
        managementService.replaceRoles(userId, request.roleIds());
    }

    @DeleteMapping("/{userId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_ADMIN_USER_WRITE)
    @AuditLog(module = "管理员管理", action = "注销管理员", resource = "admin-user",
            subjectType = "admin-user", subjectId = "#userId")
    public void delete(@PathVariable @Positive Long userId) {
        managementService.delete(userId);
    }
}
