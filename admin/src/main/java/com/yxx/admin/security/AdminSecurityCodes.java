package com.yxx.admin.security;

/**
 * 管理端角色和权限编码。
 */
public final class AdminSecurityCodes {

    /** 管理员角色。 */
    public static final String ROLE_ADMINISTRATOR = "admin:administrator";

    /** 超级管理员角色。 */
    public static final String ROLE_SUPER_ADMIN = "admin:super-admin";

    /** 查看管理端操作审计日志。 */
    public static final String PERMISSION_AUDIT_LOG_READ = "audit:log:read";

    private AdminSecurityCodes() {
    }
}
