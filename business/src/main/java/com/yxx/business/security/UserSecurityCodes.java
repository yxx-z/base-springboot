package com.yxx.business.security;

/**
 * 业务端接口权限编码。
 *
 * <p>所有鉴权注解、初始化脚本和业务判断必须引用这里的常量，禁止在代码中散落手写字符串。</p>
 */
public final class UserSecurityCodes {

    /** 查看用户端审计日志。 */
    public static final String PERMISSION_AUDIT_LOG_READ = "business:audit-log:read";

    private UserSecurityCodes() {
    }
}
