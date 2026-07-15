package com.yxx.framework.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.yxx.common.core.model.LoginUser;
import com.yxx.common.utils.auth.LoginAdminUtils;
import com.yxx.common.utils.auth.LoginUtils;
import com.yxx.common.utils.satoken.StpAdminUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yxx
 * @since 2022/4/13 14:21
 */
@Component
public class SaInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        LoginUser loginUser = getLoginUser(loginType);
        List<String> permissions = new ArrayList<>();
        permissions.addAll(nullSafe(loginUser.getMenuPermission()));
        permissions.addAll(nullSafe(loginUser.getButtonPermission()));
        return permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LoginUser loginUser = getLoginUser(loginType);
        return new ArrayList<>(nullSafe(loginUser.getRolePermission()));
    }

    /**
     * 根据 Sa-Token 多账号类型读取对应的登录会话。
     *
     * @param loginType 当前鉴权体系类型
     * @return 当前登录用户
     */
    private LoginUser getLoginUser(String loginType) {
        if (StpAdminUtil.TYPE.equals(loginType)) {
            return LoginAdminUtils.getLoginUser();
        }
        return LoginUtils.getLoginUser();
    }

    /**
     * 将可能为空的权限集合转换为空集合，防止尚未配置按钮权限时鉴权出现空指针。
     *
     * @param values 权限或角色集合
     * @return 非空集合
     */
    private List<String> nullSafe(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
