package com.yxx.rbac.model;

import com.yxx.security.constant.SecurityRealm;

import java.util.Arrays;

/**
 * 可参与 RBAC 授权的主体类型。
 *
 * <p>主体类型同时声明其唯一允许关联的权限域，由公共服务集中执行校验，避免仅依靠角色编码
 * 前缀等松散约定造成业务用户获得管理端角色。</p>
 */
public enum RbacSubjectType {

    /** 管理后台账号。 */
    ADMIN_USER(SecurityRealm.ADMIN, RbacScope.ADMIN),

    /** 业务端注册用户。 */
    BUSINESS_USER(SecurityRealm.USER, RbacScope.BUSINESS);

    private final String code;
    private final RbacScope scope;

    RbacSubjectType(String code, RbacScope scope) {
        this.code = code;
        this.scope = scope;
    }

    public String code() {
        return code;
    }

    public RbacScope scope() {
        return scope;
    }

    /** 根据认证域解析主体类型，未知认证域不能进入 RBAC 查询。 */
    public static RbacSubjectType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知 RBAC 主体类型：" + code));
    }
}
