package com.yxx.business.auth;

import com.yxx.business.auth.model.AuthorizationSnapshot;
import com.yxx.business.model.entity.Permission;
import com.yxx.business.model.entity.Role;
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
        // 角色、权限分两次批量查询，避免按角色逐一读取造成 N+1。
        List<Integer> roleIds = userRoleService.listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new AuthorizationSnapshot(Collections.emptySet(), Collections.emptySet());
        }

        // LinkedHashSet 提供去重和稳定迭代顺序，适合作为会话中的权限快照。
        Set<String> roles = roleService.findByIds(roleIds).stream()
                .map(Role::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<Integer> permissionIds = rolePermissionService.listPermissionIdsByRoleIds(roleIds);
        if (permissionIds.isEmpty()) {
            // 无权限不等于无角色，角色集合仍需保留用于角色级鉴权。
            return new AuthorizationSnapshot(roles, Collections.emptySet());
        }

        // 服务查询和此处状态过滤形成双重防御，不把停用权限写入会话。
        Set<String> permissions = permissionService.findActiveByIds(permissionIds).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getStatus()))
                .map(Permission::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new AuthorizationSnapshot(roles, permissions);
    }
}
