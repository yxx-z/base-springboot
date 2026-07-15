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
        List<Integer> roleIds = userRoleService.listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new Snapshot(Collections.emptySet(), Collections.emptySet());
        }

        Set<String> roles = roleService.listByIds(roleIds).stream()
                .map(AdminRole::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<Integer> permissionIds = rolePermissionService.listPermissionIdsByRoleIds(roleIds);
        if (permissionIds.isEmpty()) {
            return new Snapshot(roles, Collections.emptySet());
        }
        Set<String> permissions = permissionService.listByIds(permissionIds).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getStatus()))
                .map(AdminPermission::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new Snapshot(roles, permissions);
    }

    /** 管理端授权快照。 */
    public record Snapshot(Set<String> roles, Set<String> permissions) {
    }
}
