package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.RolePermissionMapper;
import com.yxx.business.model.entity.RolePermission;
import com.yxx.business.service.RolePermissionService;
import com.yxx.business.service.UserRoleService;
import com.yxx.business.service.PermissionService;
import com.yxx.business.service.RoleService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
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

    private final RoleService roleService;

    private final PermissionService permissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePermissions(Integer roleId, Collection<Integer> permissionIds) {
        // 角色及目标权限全部验证通过后再改关联，避免非法 ID 导致原权限丢失。
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, roleService.findByIds(List.of(roleId)).size() == 1);
        // 权限 ID 去重，既规范目标集合也避免唯一约束冲突。
        List<Integer> distinctPermissionIds = permissionIds == null
                ? List.of()
                : permissionIds.stream().distinct().toList();
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID,
                permissionService.findActiveByIds(distinctPermissionIds).size()
                        == distinctPermissionIds.size());
        // 最终集合语义使用删除后批量重建；传空集合表示明确清空权限。
        remove(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (!distinctPermissionIds.isEmpty()) {
            List<RolePermission> relations = distinctPermissionIds.stream().map(permissionId -> {
                RolePermission relation = new RolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                return relation;
            }).toList();
            ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, saveBatch(relations));
        }

        // 采用权限快照模式时，角色权限变更必须使持有该角色的在线用户重新登录。
        userRoleService.listUserIdsByRoleId(roleId).stream()
                .forEach(sessionInvalidationService::invalidateUserAfterCommit);
    }

    @Override
    public List<Integer> listPermissionIdsByRoleIds(Collection<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            // 避免生成 SQL IN ()，并统一返回不可变空集合。
            return List.of();
        }
        return list(new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
                .stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
    }
}
