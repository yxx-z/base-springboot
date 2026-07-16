package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminRolePermissionMapper;
import com.yxx.admin.model.entity.AdminRolePermission;
import com.yxx.admin.model.entity.AdminRole;
import com.yxx.admin.service.AdminRolePermissionService;
import com.yxx.admin.service.AdminUserRoleService;
import com.yxx.admin.service.AdminRoleService;
import com.yxx.admin.service.AdminPermissionService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.admin.security.AdminSecurityCodes;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.security.context.SessionInvalidationService;
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
    private final SessionInvalidationService sessionInvalidationService;

    private final AdminRoleService roleService;

    private final AdminPermissionService permissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePermissions(Integer roleId, Collection<Integer> permissionIds) {
        AdminRole role = roleService.findByIds(List.of(roleId)).stream().findFirst().orElse(null);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, role != null);
        ApiAssert.isTrue(ApiCode.BUILT_IN_ROLE_IMMUTABLE,
                !AdminSecurityCodes.ROLE_SUPER_ADMIN.equals(role.getCode()));
        List<Integer> distinctPermissionIds = permissionIds == null
                ? List.of()
                : permissionIds.stream().distinct().toList();
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID,
                permissionService.findActiveByIds(distinctPermissionIds).size()
                        == distinctPermissionIds.size());
        remove(new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId));
        if (!distinctPermissionIds.isEmpty()) {
            List<AdminRolePermission> relations = distinctPermissionIds.stream().map(permissionId -> {
                AdminRolePermission relation = new AdminRolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                return relation;
            }).toList();
            ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, saveBatch(relations));
        }
        userRoleService.listUserIdsByRoleId(roleId).stream()
                .forEach(sessionInvalidationService::invalidateAdminAfterCommit);
    }

    @Override
    public List<Integer> listPermissionIdsByRoleIds(Collection<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<AdminRolePermission>()
                        .in(AdminRolePermission::getRoleId, roleIds))
                .stream()
                .map(AdminRolePermission::getPermissionId)
                .distinct()
                .toList();
    }
}
