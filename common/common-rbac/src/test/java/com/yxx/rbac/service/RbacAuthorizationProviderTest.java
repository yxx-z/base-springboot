package com.yxx.rbac.service;

import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.constant.RbacSecurityCodes;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.model.entity.RbacRole;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.model.AuthorizationSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

/** 统一 RBAC 超级管理员授权安全不变量测试。 */
class RbacAuthorizationProviderTest {

    @Test
    void shouldGrantWildcardPermissionToAdminSuperRole() {
        RbacSubjectRoleService subjectRoleService = mock(RbacSubjectRoleService.class);
        RbacRoleService roleService = mock(RbacRoleService.class);
        RbacRolePermissionService rolePermissionService = mock(RbacRolePermissionService.class);
        RbacPermissionService permissionService = mock(RbacPermissionService.class);
        RbacAuthorizationProvider provider = new RbacAuthorizationProvider(
                subjectRoleService, roleService, rolePermissionService, permissionService);

        RbacRole superAdmin = new RbacRole();
        superAdmin.setId(1);
        superAdmin.setScope(RbacScope.ADMIN.code());
        superAdmin.setCode(RbacSecurityCodes.ROLE_ADMIN_SUPER_ADMIN);
        superAdmin.setSuperRole(Boolean.TRUE);
        when(subjectRoleService.listRoleIdsBySubject(
                RbacSubjectType.ADMIN_USER.code(), 10L)).thenReturn(List.of(1));
        when(roleService.findByIds(RbacScope.ADMIN, List.of(1)))
                .thenReturn(List.of(superAdmin));
        when(roleService.isCanonicalSuperRole(superAdmin)).thenReturn(true);

        AuthorizationSnapshot snapshot = provider.load(SecurityRealm.ADMIN, 10L);

        assertEquals(Set.of(RbacSecurityCodes.ROLE_ADMIN_SUPER_ADMIN), snapshot.roles());
        assertEquals(Set.of(RbacSecurityCodes.PERMISSION_ALL), snapshot.permissions());
        verify(rolePermissionService, never())
                .listPermissionIdsByRoleIds(RbacScope.ADMIN, List.of(1));
    }

    @Test
    void shouldNotGrantPermissionsFromLogicallyDeletedRoleIds() {
        RbacSubjectRoleService subjectRoleService = mock(RbacSubjectRoleService.class);
        RbacRoleService roleService = mock(RbacRoleService.class);
        RbacRolePermissionService rolePermissionService = mock(RbacRolePermissionService.class);
        RbacPermissionService permissionService = mock(RbacPermissionService.class);
        RbacAuthorizationProvider provider = new RbacAuthorizationProvider(
                subjectRoleService, roleService, rolePermissionService, permissionService);

        when(subjectRoleService.listRoleIdsBySubject(
                RbacSubjectType.BUSINESS_USER.code(), 20L)).thenReturn(List.of(99));
        // MyBatis-Plus 逻辑删除过滤后，有关联记录但加载不到有效角色。
        when(roleService.findByIds(RbacScope.BUSINESS, List.of(99))).thenReturn(List.of());

        AuthorizationSnapshot snapshot = provider.load(SecurityRealm.USER, 20L);

        assertEquals(Set.of(), snapshot.roles());
        assertEquals(Set.of(), snapshot.permissions());
        verifyNoInteractions(rolePermissionService, permissionService);
    }
}
