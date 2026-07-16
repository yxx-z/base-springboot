package com.yxx.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.rbac.mapper.RbacRolePermissionMapper;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.model.entity.RbacRole;
import com.yxx.rbac.model.entity.RbacRolePermission;
import com.yxx.security.context.SessionInvalidationReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 统一角色权限关联服务，负责强制执行资源权限域一致性。 */
@Service
@RequiredArgsConstructor
public class RbacRolePermissionService {

    private final RbacRolePermissionMapper rolePermissionMapper;
    private final RbacRoleService roleService;
    private final RbacPermissionService permissionService;
    private final RbacSubjectRoleService subjectRoleService;

    /** 替换角色权限，并使所有持有该角色的旧授权快照失效。 */
    @Transactional(rollbackFor = Exception.class)
    public void replacePermissions(Integer roleId, Collection<Integer> permissionIds) {
        RbacRole role = getRequiredRole(roleId);
        ApiAssert.isTrue(ApiCode.BUILT_IN_ROLE_IMMUTABLE,
                !Boolean.TRUE.equals(role.getBuiltIn()));

        RbacScope scope = RbacScope.fromCode(role.getScope());
        List<Integer> distinctPermissionIds = permissionIds == null
                ? List.of()
                : permissionIds.stream().distinct().toList();
        ApiAssert.isTrue(ApiCode.RBAC_SCOPE_MISMATCH,
                permissionService.findActiveByIds(scope, distinctPermissionIds).size()
                        == distinctPermissionIds.size());

        rolePermissionMapper.delete(new LambdaQueryWrapper<RbacRolePermission>()
                .eq(RbacRolePermission::getRoleId, roleId));
        if (!distinctPermissionIds.isEmpty()) {
            List<RbacRolePermission> relations = distinctPermissionIds.stream()
                    .map(permissionId -> {
                        RbacRolePermission relation = new RbacRolePermission();
                        relation.setScope(scope.code());
                        relation.setRoleId(roleId);
                        relation.setPermissionId(permissionId);
                        return relation;
                    }).toList();
            int inserted = relations.stream().mapToInt(rolePermissionMapper::insert).sum();
            ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, inserted == relations.size());
        }

        subjectRoleService.listSubjectsByRoleId(roleId)
                .forEach(subject -> subjectRoleService.invalidateAfterCommit(
                        subject, SessionInvalidationReason.ROLE_PERMISSION_CHANGED));
    }

    /** 批量查询角色拥有的权限主键。 */
    public List<Integer> listPermissionIdsByRoleIds(
            RbacScope scope, Collection<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<RbacRolePermission>()
                .eq(RbacRolePermission::getScope, scope.code())
                .in(RbacRolePermission::getRoleId, roleIds)).stream()
                .map(RbacRolePermission::getPermissionId)
                .distinct()
                .toList();
    }

    /** 批量按角色分组权限主键，供配置列表避免 N+1 查询。 */
    public Map<Integer, List<Integer>> mapPermissionIdsByRoleIds(
            RbacScope scope, Collection<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        rolePermissionMapper.selectList(new LambdaQueryWrapper<RbacRolePermission>()
                .eq(RbacRolePermission::getScope, scope.code())
                .in(RbacRolePermission::getRoleId, roleIds)).forEach(relation ->
                result.computeIfAbsent(relation.getRoleId(), ignored ->
                        new java.util.ArrayList<>()).add(relation.getPermissionId()));
        return result;
    }

    private RbacRole getRequiredRole(Integer roleId) {
        RbacRole role = roleService.findById(roleId).orElse(null);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, role != null);
        return role;
    }
}
