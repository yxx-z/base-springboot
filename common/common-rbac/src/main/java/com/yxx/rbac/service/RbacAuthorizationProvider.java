package com.yxx.rbac.service;

import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.constant.RbacSecurityCodes;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.model.entity.RbacPermission;
import com.yxx.rbac.model.entity.RbacRole;
import com.yxx.security.authorization.AuthorizationProvider;
import com.yxx.security.model.AuthorizationSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 基于统一 RBAC 表实现的授权信息提供器。 */
@Service
@RequiredArgsConstructor
public class RbacAuthorizationProvider implements AuthorizationProvider {

    private final RbacSubjectRoleService subjectRoleService;
    private final RbacRoleService roleService;
    private final RbacRolePermissionService rolePermissionService;
    private final RbacPermissionService permissionService;

    @Override
    public AuthorizationSnapshot load(String realm, Long subjectId) {
        RbacSubjectType subjectType = RbacSubjectType.fromCode(realm);
        RbacScope scope = subjectType.scope();
        List<Integer> roleIds = subjectRoleService.listRoleIdsBySubject(
                subjectType.code(), subjectId);
        if (roleIds.isEmpty()) {
            return AuthorizationSnapshot.empty();
        }

        List<RbacRole> roles = roleService.findByIds(scope, roleIds);
        Set<String> roleCodes = roles.stream()
                .map(RbacRole::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        boolean hasSuperRole = scope == RbacScope.ADMIN && roles.stream()
                .anyMatch(role -> Boolean.TRUE.equals(role.getSuperRole()));
        if (hasSuperRole) {
            return new AuthorizationSnapshot(
                    roleCodes, Set.of(RbacSecurityCodes.PERMISSION_ALL));
        }

        List<Integer> permissionIds = rolePermissionService
                .listPermissionIdsByRoleIds(scope, roleIds);
        Set<String> permissions = permissionService
                .findActiveByIds(scope, permissionIds).stream()
                .map(RbacPermission::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new AuthorizationSnapshot(roleCodes, permissions);
    }
}
