package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.RolePermissionMapper;
import com.yxx.business.model.entity.RolePermission;
import com.yxx.business.service.RolePermissionService;
import com.yxx.business.service.UserRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.security.context.SessionInvalidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/** 用户端角色权限关联服务实现。 */
@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission>
        implements RolePermissionService {

    private final UserRoleService userRoleService;
    private final SessionInvalidationService sessionInvalidationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePermissions(Integer roleId, Collection<Integer> permissionIds) {
        remove(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> relations = permissionIds.stream().distinct().map(permissionId -> {
                RolePermission relation = new RolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                return relation;
            }).toList();
            saveBatch(relations);
        }

        // 采用权限快照模式时，角色权限变更必须使持有该角色的在线用户重新登录。
        userRoleService.listUserIdsByRoleId(roleId).stream()
                .forEach(sessionInvalidationService::invalidateUserAfterCommit);
    }

    @Override
    public List<Integer> listPermissionIdsByRoleIds(Collection<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
                .stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
    }
}
