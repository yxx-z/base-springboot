package com.yxx.business.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册验证码
 *
 * @author yxx
 * @classname RegisterCaptchaReq
 * @since 2023-07-25 20:53
 */
@Data
public class RegisterCaptchaReq {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "请输入正确的邮箱地址")
    @Size(max = 100, message = "邮箱长度不能超过100位")
    private String email;
}
