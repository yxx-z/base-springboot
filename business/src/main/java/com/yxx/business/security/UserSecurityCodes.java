package com.yxx.business.security;

/**
 * 用户端角色和权限编码。
 *
 * <p>所有鉴权注解、初始化脚本和业务判断必须引用这里的常量，禁止在代码中散落手写字符串。</p>
 */
public final class UserSecurityCodes {

    /** 普通用户默认角色。 */
    public static final String ROLE_MEMBER = "user:member";

    /** 用户端管理角色。 */
    public static final String ROLE_ADMINISTRATOR = "user:administrator";

    /** 查看用户端审计日志。 */
    public static final String PERMISSION_AUDIT_LOG_READ = "audit:log:read";

    private UserSecurityCodes() {
    }
}
