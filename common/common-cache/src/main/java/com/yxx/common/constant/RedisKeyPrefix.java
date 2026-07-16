package com.yxx.common.constant;

/** Redis 业务 Key 前缀，所有动态标识都追加在对应前缀之后。 */
public final class RedisKeyPrefix {

    public static final String EMAIL_REGISTER = "email:register:";
    public static final String EMAIL_REGISTER_NUM = "email:register:num:";
    public static final String USER_RESET_PASSWORD_COUNT = "user:reset:password:count:";
    public static final String ADMIN_RESET_PASSWORD_COUNT = "admin:reset:password:count:";
    public static final String USER_RESET_PASSWORD_TOKEN = "user:reset:password:token:";
    public static final String ADMIN_RESET_PASSWORD_TOKEN = "admin:reset:password:token:";
    public static final String USER_LOGIN_RISK = "security:login-risk:user:";
    public static final String ADMIN_LOGIN_RISK = "security:login-risk:admin:";

    private RedisKeyPrefix() {
    }
}
