package com.yxx.business.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

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
    private String loginCode;

    /**
     * 登录密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
