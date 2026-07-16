package com.yxx.admin.model.request;

import com.yxx.security.validation.Password;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author yxx
 * @since 2022-11-12 14:00
 */
@Data
public class LoginReq {
    /**
     * 登录账号
     */
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 50, message = "登录账号长度不能超过50位")
    private String loginCode;

    /**
     * 登录密码
     */
    @NotBlank(message = "密码不能为空")
    @Password(enforcePolicy = false, message = "密码长度超过系统限制")
    private String password;
}
