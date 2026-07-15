package com.yxx.framework.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.model.LoginPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
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
        return loginSessionService.findByLoginId(loginType, loginId)
                .map(LoginPrincipal::getPermissions)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return loginSessionService.findByLoginId(loginType, loginId)
                .map(LoginPrincipal::getRoles)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }
}
