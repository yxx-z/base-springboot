package com.yxx.business.model.request;

import com.yxx.security.validation.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
    @Size(min = 4, max = 50, message = "登录账号应为4-50位")
    private String loginCode;

    /**
     * 登录名
     */
    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 50, message = "昵称应为2-50位")
    private String loginName;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Password
    private String password;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的中国大陆手机号")
    private String linkPhone;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "请输入正确的邮箱地址")
    @Size(max = 100, message = "邮箱长度不能超过100位")
    private String email;


    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 10, message = "验证码格式不正确")
    private String captcha;
}
