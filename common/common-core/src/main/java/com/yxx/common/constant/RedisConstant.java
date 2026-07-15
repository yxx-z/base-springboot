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
    String RESET_PWD_NUM = "reset:pwd:num:";

    /**
     * 重置密码内容
     */
    String RESET_PWD_CONTENT = "reset:pwd:content:";

    /**
     * ip异常操作
     */
    String IP_UNUSUAL_OPERATE = "ip:unusual:operate:";

    /**
     * ip异常登录
     */
    String IP_UNUSUAL_LOGIN = "ip:unusual:login:";
}
