package com.yxx.rbac.constant;

/** RBAC 内置角色和权限编码。 */
public final class RbacSecurityCodes {

    /** 业务用户注册或首次第三方登录时获得的默认角色。 */
    public static final String ROLE_BUSINESS_MEMBER = "business:member";

    /** 管理端内置超级管理员角色。 */
    public static final String ROLE_ADMIN_SUPER_ADMIN = "admin:super-admin";

    /** Sa-Token 识别的全部权限通配符。 */
    public static final String PERMISSION_ALL = "*";

    private RbacSecurityCodes() {
    }
}
