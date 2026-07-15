package com.yxx.framework.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.yxx.common.core.model.LoginUser;
import com.yxx.common.utils.auth.LoginUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yxx
 * @since 2022/4/13 14:21
 */
@Component
public class SaInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        LoginUser loginUser = LoginUtils.getLoginUser();
        List<String> permissions = loginUser.getMenuPermission();
        permissions.addAll(loginUser.getButtonPermission());
        return new ArrayList<>(permissions);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LoginUser loginUser = LoginUtils.getLoginUser();
        return new ArrayList<>(loginUser.getRolePermission());
    }
}
