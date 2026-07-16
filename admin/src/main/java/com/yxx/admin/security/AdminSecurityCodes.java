package com.yxx.admin.security;

/**
 * 管理端接口权限编码。公共内置角色编码统一由 common-rbac 维护。
 */
public final class AdminSecurityCodes {

    /** 查看管理端操作审计日志。 */
    public static final String PERMISSION_AUDIT_LOG_READ = "admin:audit-log:read";

    /** 查看业务用户及其角色。 */
    public static final String PERMISSION_BUSINESS_USER_READ = "admin:business-user:read";

    /** 配置业务用户角色。 */
    public static final String PERMISSION_BUSINESS_USER_ROLE_WRITE =
            "admin:business-user:role:write";

    /** 查看统一 RBAC 配置。 */
    public static final String PERMISSION_RBAC_READ = "admin:rbac:read";

    /** 修改统一 RBAC 配置。 */
    public static final String PERMISSION_RBAC_WRITE = "admin:rbac:write";

    private AdminSecurityCodes() {
    }
}
