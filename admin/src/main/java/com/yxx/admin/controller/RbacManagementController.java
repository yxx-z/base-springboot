package com.yxx.admin.controller;

import com.yxx.admin.model.request.ReplaceMenuIdsReq;
import com.yxx.admin.model.request.ReplacePermissionIdsReq;
import com.yxx.admin.model.response.RbacMenuRes;
import com.yxx.admin.model.response.RbacPermissionRes;
import com.yxx.admin.model.response.RbacRoleRes;
import com.yxx.admin.security.AdminSecurityCodes;
import com.yxx.admin.service.RbacConfigurationQueryService;
import com.yxx.common.annotation.response.ResponseResult;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.framework.audit.annotation.AuditLog;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.service.RbacRoleMenuService;
import com.yxx.rbac.service.RbacRolePermissionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端统一 RBAC 配置接口。
 *
 * <p>admin 可以查看两个权限域，但所有写入仍由公共 RBAC 服务校验角色与资源的权限域，
 * 管理权限不等于允许制造跨域关联。</p>
 */
@Validated
@ResponseResult
@RestController
@RequestMapping("/management/rbac")
@RequiredArgsConstructor
public class RbacManagementController {

    private final RbacConfigurationQueryService queryService;
    private final RbacRolePermissionService rolePermissionService;
    private final RbacRoleMenuService roleMenuService;

    /** 查询指定权限域的角色及其当前授权关系。 */
    @GetMapping("/roles")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_READ)
    public List<RbacRoleRes> roles(@RequestParam String scope) {
        return queryService.listRoles(parseScope(scope));
    }

    /** 查询指定权限域的后端权限资源。 */
    @GetMapping("/permissions")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_READ)
    public List<RbacPermissionRes> permissions(@RequestParam String scope) {
        return queryService.listPermissions(parseScope(scope));
    }

    /** 查询指定权限域的前端菜单资源。 */
    @GetMapping("/menus")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_READ)
    public List<RbacMenuRes> menus(@RequestParam String scope) {
        return queryService.listMenus(parseScope(scope));
    }

    /** 替换角色的后端权限集合。 */
    @PutMapping("/roles/{roleId}/permissions")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "配置角色权限", resource = "rbac-role")
    public void replacePermissions(
            @PathVariable @Positive(message = "角色主键必须为正数") Integer roleId,
            @Valid @RequestBody ReplacePermissionIdsReq request) {
        rolePermissionService.replacePermissions(roleId, request.permissionIds());
    }

    /** 替换角色的前端菜单集合。 */
    @PutMapping("/roles/{roleId}/menus")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "配置角色菜单", resource = "rbac-role")
    public void replaceMenus(
            @PathVariable @Positive(message = "角色主键必须为正数") Integer roleId,
            @Valid @RequestBody ReplaceMenuIdsReq request) {
        roleMenuService.replaceMenus(roleId, request.menuIds());
    }

    private RbacScope parseScope(String scope) {
        try {
            return RbacScope.fromCode(scope == null ? null : scope.trim().toLowerCase());
        } catch (IllegalArgumentException exception) {
            // 对外部输入返回稳定业务错误，不把内部枚举解析异常作为服务器错误暴露。
            throw new ApiException(ApiCode.PARAM_IS_INVALID);
        }
    }
}
