package com.yxx.admin.security;

import com.yxx.admin.model.entity.AdminPermission;
import com.yxx.admin.model.entity.AdminRole;
import com.yxx.admin.service.AdminPermissionService;
import com.yxx.admin.service.AdminRolePermissionService;
import com.yxx.admin.service.AdminRoleService;
import com.yxx.admin.service.AdminUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理端授权快照加载服务。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private final AdminUserRoleService userRoleService;
    private final AdminRoleService roleService;
    private final AdminRolePermissionService rolePermissionService;
    private final AdminPermissionService permissionService;

    /**
     * 批量加载管理员角色和权限。
     *
     * @param userId 管理员主键
     * @return 授权快照
     */
    public Snapshot load(Long userId) {
        // 先批量取得角色 ID；无角色时遵循默认拒绝原则返回空授权。
        List<Integer> roleIds = userRoleService.listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new Snapshot(Collections.emptySet(), Collections.emptySet());
        }

        // LinkedHashSet 同时去重并保持数据库返回顺序，便于日志和测试稳定。
        Set<String> roles = roleService.findByIds(roleIds).stream()
                .map(AdminRole::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (roles.contains(AdminSecurityCodes.ROLE_SUPER_ADMIN)) {
            // 超级管理员权限由内置安全不变量保证，不依赖可被误修改的角色权限关联数据。
            return new Snapshot(roles, Set.of(AdminSecurityCodes.PERMISSION_ALL));
        }
        List<Integer> permissionIds = rolePermissionService.listPermissionIdsByRoleIds(roleIds);
        if (permissionIds.isEmpty()) {
            // 保留已加载角色，权限集合独立为空，避免混淆“无角色”和“角色无权限”。
            return new Snapshot(roles, Collections.emptySet());
        }
        // 再次过滤状态作为防御性校验，防止历史关联指向已停用权限。
        Set<String> permissions = permissionService.findActiveByIds(permissionIds).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getStatus()))
                .map(AdminPermission::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new Snapshot(roles, permissions);
    }

    /** 管理端授权快照。 */
    public record Snapshot(Set<String> roles, Set<String> permissions) {
    }
}
