package com.yxx.admin.controller;

import com.yxx.admin.model.request.ReplaceMenuIdsReq;
import com.yxx.admin.model.request.ReplacePermissionIdsReq;
import com.yxx.admin.model.request.CreateRoleReq;
import com.yxx.admin.model.request.UpdateRoleReq;
import com.yxx.admin.model.request.CreatePermissionReq;
import com.yxx.admin.model.request.UpdatePermissionReq;
import com.yxx.admin.model.request.CreateMenuReq;
import com.yxx.admin.model.request.UpdateMenuReq;
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
import com.yxx.rbac.service.RbacManagementService;
import com.yxx.rbac.model.entity.RbacMenu;
import com.yxx.security.annotation.SaAdminCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final RbacManagementService managementService;

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

    /** 新增普通角色。 */
    @PostMapping("/roles")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "新增角色", resource = "rbac-role",
            subjectType = "rbac-role", subjectAccount = "#request.code")
    public Integer createRole(@Valid @RequestBody CreateRoleReq request) {
        return managementService.createRole(
                parseScope(request.scope()), request.code(), request.name(), request.remark());
    }

    /** 修改普通角色名称和说明。 */
    @PutMapping("/roles/{roleId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "修改角色", resource = "rbac-role",
            subjectType = "rbac-role", subjectId = "#roleId")
    public void updateRole(@PathVariable @Positive Integer roleId,
                           @Valid @RequestBody UpdateRoleReq request) {
        managementService.updateRole(roleId, request.name(), request.remark());
    }

    /** 删除普通角色并撤销关联授权。 */
    @DeleteMapping("/roles/{roleId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "删除角色", resource = "rbac-role",
            subjectType = "rbac-role", subjectId = "#roleId")
    public void deleteRole(@PathVariable @Positive Integer roleId) {
        managementService.deleteRole(roleId);
    }

    /** 新增后端权限资源。 */
    @PostMapping("/permissions")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "新增权限", resource = "rbac-permission",
            subjectType = "rbac-permission", subjectAccount = "#request.code")
    public Integer createPermission(@Valid @RequestBody CreatePermissionReq request) {
        return managementService.createPermission(
                parseScope(request.scope()), request.code(), request.name(),
                request.resourceType(), request.description(), request.enabled());
    }

    /** 修改权限资料和启用状态。 */
    @PutMapping("/permissions/{permissionId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "修改权限", resource = "rbac-permission",
            subjectType = "rbac-permission", subjectId = "#permissionId")
    public void updatePermission(@PathVariable @Positive Integer permissionId,
                                 @Valid @RequestBody UpdatePermissionReq request) {
        managementService.updatePermission(permissionId, request.name(), request.resourceType(),
                request.description(), request.enabled());
    }

    /** 删除权限并使所有受影响主体的旧授权快照失效。 */
    @DeleteMapping("/permissions/{permissionId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "删除权限", resource = "rbac-permission",
            subjectType = "rbac-permission", subjectId = "#permissionId")
    public void deletePermission(@PathVariable @Positive Integer permissionId) {
        managementService.deletePermission(permissionId);
    }

    /** 新增前端导航菜单。 */
    @PostMapping("/menus")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "新增菜单", resource = "rbac-menu",
            subjectType = "rbac-menu", subjectAccount = "#request.menuCode")
    public Integer createMenu(@Valid @RequestBody CreateMenuReq request) {
        RbacMenu menu = toMenu(request);
        return managementService.createMenu(menu);
    }

    /** 修改菜单导航属性。 */
    @PutMapping("/menus/{menuId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "修改菜单", resource = "rbac-menu",
            subjectType = "rbac-menu", subjectId = "#menuId")
    public void updateMenu(@PathVariable @Positive Integer menuId,
                           @Valid @RequestBody UpdateMenuReq request) {
        RbacMenu menu = toMenu(request);
        managementService.updateMenu(menuId, menu);
    }

    /** 删除无子节点菜单并清理角色菜单关联。 */
    @DeleteMapping("/menus/{menuId}")
    @SaAdminCheckPermission(AdminSecurityCodes.PERMISSION_RBAC_WRITE)
    @AuditLog(module = "权限管理", action = "删除菜单", resource = "rbac-menu",
            subjectType = "rbac-menu", subjectId = "#menuId")
    public void deleteMenu(@PathVariable @Positive Integer menuId) {
        managementService.deleteMenu(menuId);
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

    private RbacMenu toMenu(CreateMenuReq request) {
        RbacMenu menu = new RbacMenu();
        menu.setScope(parseScope(request.scope()).code());
        menu.setParentId(request.parentId());
        menu.setMenuCode(request.menuCode());
        menu.setMenuName(request.menuName());
        menu.setPath(request.path());
        menu.setComponent(request.component());
        menu.setIcon(request.icon());
        menu.setSort(request.sort());
        menu.setVisible(request.visible());
        menu.setStatus(request.enabled());
        return menu;
    }

    private RbacMenu toMenu(UpdateMenuReq request) {
        RbacMenu menu = new RbacMenu();
        menu.setParentId(request.parentId());
        menu.setMenuName(request.menuName());
        menu.setPath(request.path());
        menu.setComponent(request.component());
        menu.setIcon(request.icon());
        menu.setSort(request.sort());
        menu.setVisible(request.visible());
        menu.setStatus(request.enabled());
        return menu;
    }
}
