package com.yxx.framework.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.model.LoginPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yxx
 * @since 2022/4/13 14:21
 */
@Component
@RequiredArgsConstructor
public class SaInterfaceImpl implements StpInterface {

    private final LoginSessionService loginSessionService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 权限直接来自登录时快照；数据库变更通过会话失效机制触发重新登录加载。
        return loginSessionService.findByLoginId(loginType, loginId)
                .map(LoginPrincipal::getPermissions)
                // 返回可变副本，避免框架内部操作影响 LoginPrincipal 的不可变集合。
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 主体快照缺失时返回空角色，遵循默认拒绝原则而不是授予兜底角色。
        return loginSessionService.findByLoginId(loginType, loginId)
                .map(LoginPrincipal::getRoles)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }
}
