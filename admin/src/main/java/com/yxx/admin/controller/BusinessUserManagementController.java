package com.yxx.admin.controller;

import com.yxx.admin.model.request.BusinessUserPageReq;
import com.yxx.admin.model.request.ReplaceRoleIdsReq;
import com.yxx.admin.model.response.ManagedBusinessUserRes;
import com.yxx.admin.security.AdminSecurityCodes;
import com.yxx.admin.service.BusinessUserAccessManagementService;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.core.page.PageResponse;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.security.annotation.SaAdminCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端对业务用户及其业务角色的管理接口。 */
@Validated
@ResponseResult
@RestController
@RequestMapping("/management/business-users")
@RequiredArgsConstructor
public class BusinessUserManagementController {

    private final BusinessUserAccessManagementService managementService;

    /** 分页查询被管理的业务用户。 */
    @GetMapping
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_BUSINESS_USER_READ)
    public PageResponse<ManagedBusinessUserRes> page(@Valid BusinessUserPageReq request) {
        return managementService.page(request);
    }

    /** 查询业务用户详情及当前角色。 */
    @GetMapping("/{userId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_BUSINESS_USER_READ)
    public ManagedBusinessUserRes detail(
            @PathVariable @Positive(message = "用户主键必须为正数") Long userId) {
        return managementService.findById(userId);
    }

    /**
     * 替换业务用户角色。
     *
     * <p>接口只接受角色主键，公共 RBAC 服务会拒绝任何 admin 权限域角色。</p>
     */
    @PutMapping("/{userId}/roles")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_BUSINESS_USER_ROLE_WRITE)
    @AuditLog(module = "业务用户管理", action = "配置业务用户角色",
            resource = "business-user", subjectField = "userId")
    public void replaceRoles(
            @PathVariable @Positive(message = "用户主键必须为正数") Long userId,
            @Valid @RequestBody ReplaceRoleIdsReq request) {
        managementService.replaceRoles(userId, request.roleIds());
    }
}
