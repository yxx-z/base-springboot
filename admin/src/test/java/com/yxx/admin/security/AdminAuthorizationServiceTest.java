package com.yxx.admin.security;

import com.yxx.admin.model.entity.AdminRole;
import com.yxx.admin.service.AdminPermissionService;
import com.yxx.admin.service.AdminRolePermissionService;
import com.yxx.admin.service.AdminRoleService;
import com.yxx.admin.service.AdminUserRoleService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 管理端授权安全不变量测试。 */
class AdminAuthorizationServiceTest {

    @Test
    void shouldGrantWildcardPermissionToBuiltInSuperAdmin() {
        AdminUserRoleService userRoleService = mock(AdminUserRoleService.class);
        AdminRoleService roleService = mock(AdminRoleService.class);
        AdminRolePermissionService rolePermissionService = mock(AdminRolePermissionService.class);
        AdminPermissionService permissionService = mock(AdminPermissionService.class);
        AdminAuthorizationService service = new AdminAuthorizationService(
                userRoleService, roleService, rolePermissionService, permissionService);

        AdminRole superAdmin = new AdminRole();
        superAdmin.setId(1);
        superAdmin.setCode(AdminSecurityCodes.ROLE_SUPER_ADMIN);
        when(userRoleService.listRoleIdsByUserId(10L)).thenReturn(List.of(1));
        when(roleService.findByIds(List.of(1))).thenReturn(List.of(superAdmin));

        AdminAuthorizationService.Snapshot snapshot = service.load(10L);

        assertEquals(Set.of(AdminSecurityCodes.ROLE_SUPER_ADMIN), snapshot.roles());
        assertEquals(Set.of(AdminSecurityCodes.PERMISSION_ALL), snapshot.permissions());
        verify(rolePermissionService, never()).listPermissionIdsByRoleIds(List.of(1));
    }
}
