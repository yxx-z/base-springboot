package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminRolePermissionMapper;
import com.yxx.admin.model.entity.AdminRolePermission;
import com.yxx.admin.service.AdminRolePermissionService;
import com.yxx.admin.model.entity.AdminUserRole;
import com.yxx.admin.service.AdminUserRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.security.context.LoginSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/** 管理端角色权限关联服务实现。 */
@Service
@RequiredArgsConstructor
public class AdminRolePermissionServiceImpl
        extends ServiceImpl<AdminRolePermissionMapper, AdminRolePermission>
        implements AdminRolePermissionService {

    private final AdminUserRoleService userRoleService;
    private final LoginSessionService loginSessionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePermissions(Integer roleId, Collection<Integer> permissionIds) {
        remove(new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<AdminRolePermission> relations = permissionIds.stream().distinct().map(permissionId -> {
                AdminRolePermission relation = new AdminRolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                return relation;
            }).toList();
            saveBatch(relations);
        }
        userRoleService.list(new LambdaQueryWrapper<AdminUserRole>()
                        .eq(AdminUserRole::getRoleId, roleId))
                .stream()
                .map(AdminUserRole::getUserId)
                .distinct()
                .forEach(loginSessionService::invalidateAdmin);
    }
}
