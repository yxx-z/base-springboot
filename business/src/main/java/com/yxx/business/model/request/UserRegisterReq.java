package com.yxx.business.model.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @author yxx
 * @since 2023-05-15 14:11
 */
@Data
public class UserRegisterReq {
    /**
     * 登录账号
     */
    @NotBlank(message = "登录账号不能为空")
    @Length(min = 4, max = 12, message = "登录账号应为4-12位")
    private String loginCode;

    /**
     * 登录名
     */
    @NotBlank(message = "昵称不能为空")
    @Length(min = 2, max = 8, message = "昵称应为2-8位")
    private String loginName;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Length(min = 8, max = 20, message = "密码应为8-20位")
    private String password;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1[3456789]\\d{9}$", message = "请输入正确手机号")
    private String linkPhone;

    /**
     * 邮箱
     */
    @Pattern(regexp = "^$|^[A-Za-z0-9]+([_.][A-Za-z0-9]+)*@((qq|163|gmail|88|email)+\\.)+[A-Za-z]{2,6}$", message = "请输入正确邮箱号,目前仅支持 qq邮箱、163邮箱、谷歌邮箱等常用邮箱")
    private String email;


    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captcha;
}
