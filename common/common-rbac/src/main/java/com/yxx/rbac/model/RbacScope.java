package com.yxx.rbac.model;

import java.util.Arrays;

/**
 * RBAC 权限域。
 *
 * <p>一套 RBAC 表可以服务多个应用入口，但不同入口的角色、权限和菜单必须具有明确边界。
 * 权限域不是另一套 RBAC，而是统一模型中的强制隔离维度。</p>
 */
public enum RbacScope {

    /** 管理后台权限域。 */
    ADMIN("admin"),

    /** 业务用户端权限域。 */
    BUSINESS("business");

    private final String code;

    RbacScope(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** 根据数据库编码解析权限域，未知编码视为配置错误。 */
    public static RbacScope fromCode(String code) {
        return Arrays.stream(values())
                .filter(scope -> scope.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知 RBAC 权限域：" + code));
    }
}
