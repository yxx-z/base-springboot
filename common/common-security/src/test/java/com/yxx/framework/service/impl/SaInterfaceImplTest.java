package com.yxx.framework.service.impl;

import com.yxx.security.context.LoginSessionService;
import com.yxx.security.model.LoginPrincipal;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sa-Token 统一主体权限读取测试。
 */
class SaInterfaceImplTest {

    private final LoginSessionService loginSessionService = mock(LoginSessionService.class);
    private final SaInterfaceImpl saInterface = new SaInterfaceImpl(loginSessionService);

    @Test
    void shouldReadRolesFromUnifiedPrincipal() {
        LoginPrincipal principal = LoginPrincipal.builder()
                .subjectId(1L)
                .subjectType("admin")
                .roles(new LinkedHashSet<>(Set.of("admin:super-admin")))
                .build();
        when(loginSessionService.findByLoginId("admin", 1L)).thenReturn(Optional.of(principal));

        assertEquals(List.of("admin:super-admin"), saInterface.getRoleList(1L, "admin"));
    }

    @Test
    void shouldReturnEmptyPermissionsWhenSessionPrincipalIsMissing() {
        when(loginSessionService.findByLoginId("user", 1L)).thenReturn(Optional.empty());

        assertEquals(List.of(), saInterface.getPermissionList(1L, "user"));
    }

    @Test
    void shouldFailClosedForUnknownLoginType() {
        when(loginSessionService.findByLoginId("unknown", 1L)).thenReturn(Optional.empty());

        assertEquals(List.of(), saInterface.getPermissionList(1L, "unknown"));
        assertEquals(List.of(), saInterface.getRoleList(1L, "unknown"));
    }
}
