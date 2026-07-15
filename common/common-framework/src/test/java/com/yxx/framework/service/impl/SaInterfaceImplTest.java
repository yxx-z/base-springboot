package com.yxx.framework.service.impl;

import com.yxx.common.core.model.LoginUser;
import com.yxx.common.utils.auth.LoginAdminUtils;
import com.yxx.common.utils.auth.LoginUtils;
import com.yxx.common.utils.satoken.StpAdminUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sa-Token 多账号权限读取测试。
 */
class SaInterfaceImplTest {

    private final SaInterfaceImpl saInterface = new SaInterfaceImpl();

    @Test
    void shouldReadAdminRolesFromAdminSession() {
        LoginUser admin = new LoginUser();
        admin.setRolePermission(List.of("super_admin"));

        try (MockedStatic<LoginAdminUtils> adminUtils = Mockito.mockStatic(LoginAdminUtils.class)) {
            adminUtils.when(LoginAdminUtils::getLoginUser).thenReturn(admin);

            assertEquals(List.of("super_admin"), saInterface.getRoleList("admin", StpAdminUtil.TYPE));
        }
    }

    @Test
    void shouldTreatMissingButtonPermissionsAsEmptyCollection() {
        LoginUser user = new LoginUser();
        user.setMenuPermission(List.of("user:view"));
        user.setButtonPermission(null);

        try (MockedStatic<LoginUtils> loginUtils = Mockito.mockStatic(LoginUtils.class)) {
            loginUtils.when(LoginUtils::getLoginUser).thenReturn(user);

            assertEquals(List.of("user:view"), saInterface.getPermissionList("user", "login"));
        }
    }
}
