package com.yxx.common.enums;

import lombok.Getter;

/**
 * @author yxx
 * @description: 状态枚举类
 */
@Getter
public enum ApiCode {
    /**
     * 成功状态码
     */
    SUCCESS(200, "成功"),

    TOKEN_ERROR(401, "token不存在或失效"),

    /**
     * 参数错误
     */
    PARAM_IS_INVALID(1001, "参数无效"),

    PARAM_IS_BLANK(1002, "参数为空"),

    PARAM_ERROR(1003, "参数错误"),

    PARAM_TYPE_BIND_ERROR(1004, "参数类型错误"),

    PARAM_NOT_COMPLETE(1005, "参数缺失"),

    /**
     * 用户错误 2001-2999
     */
    USER_NOT_LOGIN(2001, "用户未登录"),

    USER_NOT_EXIST(2002, "账号不存在"),

    USER_EXIST(2003, "账号或手机号已存在"),

    PASSWORD_ERROR(2004, "密码错误"),

    USER_PERMISSION_ERROR(2005, "无权查看他人信息"),
    USER_NOT_ROLE(2006, "用户无角色"),

    /**
     * 邮件错误
     */
    MAIL_ERROR(8000, "邮件发送失败"),
    MAIL_EXIST(8001, "邮件已成功发送过，请前往邮箱查看，如收件箱不存在，请前往垃圾邮箱查看"),

    DATE_ERROR(9000, "开始时间存在多个"),

    ENCRYPTION_ERROR(90001, "加密异常"),

    DECODE_ERROR(90002, "解密异常"),

    KEY_ERROR(90003, "密钥异常"),

    KEY_LENGTH_ERROR(90004, "加密失败，key不能小于8位"),

    SYSTEM_ERROR(10000, "系统异常，请稍后重试"),
    ;

    private final Integer code;
    private final String message;

    ApiCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer code() {
        return this.code;
    }

    public String message() {
        return this.message;
    }
}