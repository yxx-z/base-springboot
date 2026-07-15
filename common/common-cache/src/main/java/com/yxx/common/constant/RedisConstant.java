package com.yxx.common.constant;

/**
 * redis常数
 *
 * @author yxx
 * @classname RedisConstant
 * @since 2023/07/05
 */
public interface RedisConstant {

    /**
     * 注册验证码
     */
    String EMAIL_REGISTER = "email:register:";

    /**
     * 当日发送注册验证码次数
     */
    String EMAIL_REGISTER_NUM = "email:register:num:";

    /**
     * 当日找回密码次数
     */
    String USER_RESET_PWD_NUM = "user:reset:pwd:num:";

    /** 管理端当日找回密码次数。 */
    String ADMIN_RESET_PWD_NUM = "admin:reset:pwd:num:";

    /**
     * 重置密码内容
     */
    String USER_RESET_PWD_CONTENT = "user:reset:pwd:content:";

    /** 管理端重置密码邮件发送占位及令牌。 */
    String ADMIN_RESET_PWD_CONTENT = "admin:reset:pwd:content:";

    /**
     * ip异常登录
     */
    String IP_UNUSUAL_LOGIN = "ip:unusual:login:";
}
