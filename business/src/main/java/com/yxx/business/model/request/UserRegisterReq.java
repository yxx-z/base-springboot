package com.yxx.business.model.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * @author yxx
 * @since 2023-05-15 14:11
 */
@Data
public class UserRegisterReq {
    /**
     * 登录账号
     */
    @NotEmpty(message = "登录账号不能为空")
    @Size(min = 4, max = 12, message = "登录账号应为4-12位")
    private String loginCode;

    /**
     * 登录名
     */
    @NotEmpty(message = "昵称不能为空")
    @Size(min = 2, max = 8, message = "昵称应为2-8位")
    private String loginName;

    /**
     * 密码
     */
    @NotEmpty(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码应为8-20位")
    private String password;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1[3456789]\\d{9}$", message = "请输入正确手机号")
    private String linkPhone;

    /**
     * 邮箱
     */
    @Pattern(regexp = "^[A-Za-z0-9]+([_.][A-Za-z0-9]+)*@([A-Za-z0-9\\-]+\\.)+[A-Za-z]{2,6}$", message = "请输入正确邮箱号")
    private String email;


    /**
     * 验证码
     */
    @NotEmpty(message = "验证码不能为空")
    private String captcha;
}
