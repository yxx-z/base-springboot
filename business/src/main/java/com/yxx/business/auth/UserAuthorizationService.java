package com.yxx.business.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.business.auth.model.AuthorizationSnapshot;
import com.yxx.business.model.entity.Permission;
import com.yxx.business.model.entity.Role;
import com.yxx.business.model.entity.RolePermission;
import com.yxx.business.model.entity.UserRole;
import com.yxx.business.service.PermissionService;
import com.yxx.business.service.RolePermissionService;
import com.yxx.business.service.RoleService;
import com.yxx.business.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户端授权快照加载服务。
 *
 * <p>使用批量查询替代逐角色查询，消除 N+1；角色和权限使用集合去重，保证一个权限被多个
 * 角色授予时不会在 Session 中重复保存。</p>
 */
@Service
@RequiredArgsConstructor
public class UserAuthorizationService {

    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;
    private final PermissionService permissionService;

    /**
     * 加载指定用户的角色和权限快照。
     *
     * @param userId 用户内部主键
     * @return 授权快照
     */
    public AuthorizationSnapshot load(Long userId) {
        List<Integer> roleIds = userRoleService.list(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId))
                .stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return new AuthorizationSnapshot(Collections.emptySet(), Collections.emptySet());
        }

        Set<String> roles = roleService.listByIds(roleIds).stream()
                .map(Role::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<Integer> permissionIds = rolePermissionService.list(
                        new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
                .stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
        if (permissionIds.isEmpty()) {
            return new AuthorizationSnapshot(roles, Collections.emptySet());
        }

        Set<String> permissions = permissionService.listByIds(permissionIds).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getStatus()))
                .map(Permission::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new AuthorizationSnapshot(roles, permissions);
    }
}
