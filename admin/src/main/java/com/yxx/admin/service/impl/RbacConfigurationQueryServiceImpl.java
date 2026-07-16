package com.yxx.admin.service.impl;

import com.yxx.admin.model.response.RbacMenuRes;
import com.yxx.admin.model.response.RbacPermissionRes;
import com.yxx.admin.model.response.RbacRoleRes;
import com.yxx.admin.service.RbacConfigurationQueryService;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.model.entity.RbacMenu;
import com.yxx.rbac.model.entity.RbacPermission;
import com.yxx.rbac.model.entity.RbacRole;
import com.yxx.rbac.service.RbacMenuService;
import com.yxx.rbac.service.RbacPermissionService;
import com.yxx.rbac.service.RbacRoleMenuService;
import com.yxx.rbac.service.RbacRolePermissionService;
import com.yxx.rbac.service.RbacRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 统一 RBAC 配置查询实现。 */
@Service
@RequiredArgsConstructor
public class RbacConfigurationQueryServiceImpl implements RbacConfigurationQueryService {

    private final RbacRoleService roleService;
    private final RbacPermissionService permissionService;
    private final RbacMenuService menuService;
    private final RbacRolePermissionService rolePermissionService;
    private final RbacRoleMenuService roleMenuService;

    @Override
    public List<RbacRoleRes> listRoles(RbacScope scope) {
        List<RbacRole> roles = roleService.listByScope(scope);
        List<Integer> roleIds = roles.stream().map(RbacRole::getId).toList();
        Map<Integer, List<Integer>> permissionIds = rolePermissionService
                .mapPermissionIdsByRoleIds(scope, roleIds);
        Map<Integer, List<Integer>> menuIds = roleMenuService
                .mapMenuIdsByRoleIds(scope, roleIds);
        return roles.stream()
                .map(role -> toRoleResponse(
                        role,
                        permissionIds.getOrDefault(role.getId(), List.of()),
                        menuIds.getOrDefault(role.getId(), List.of())))
                .toList();
    }

    @Override
    public List<RbacPermissionRes> listPermissions(RbacScope scope) {
        return permissionService.listByScope(scope).stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    @Override
    public List<RbacMenuRes> listMenus(RbacScope scope) {
        return menuService.listByScope(scope).stream()
                .map(this::toMenuResponse)
                .toList();
    }

    private RbacRoleRes toRoleResponse(
            RbacRole role, List<Integer> permissionIds, List<Integer> menuIds) {
        return new RbacRoleRes(
                role.getId(), role.getScope(), role.getCode(), role.getName(), role.getRemark(),
                role.getBuiltIn(), role.getSuperRole(), permissionIds, menuIds);
    }

    private RbacPermissionRes toPermissionResponse(RbacPermission permission) {
        return new RbacPermissionRes(
                permission.getId(), permission.getScope(), permission.getCode(),
                permission.getName(), permission.getResourceType(),
                permission.getDescription(), permission.getStatus());
    }

    private RbacMenuRes toMenuResponse(RbacMenu menu) {
        return new RbacMenuRes(
                menu.getId(), menu.getScope(), menu.getParentId(), menu.getMenuCode(),
                menu.getMenuName(), menu.getPath(), menu.getComponent(), menu.getIcon(),
                menu.getSort(), menu.getVisible(), menu.getStatus());
    }
}
