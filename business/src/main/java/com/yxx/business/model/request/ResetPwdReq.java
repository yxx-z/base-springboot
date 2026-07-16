package com.yxx.business.model.request;

import com.yxx.security.validation.Password;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求参数
 *
 * @author yxx
 * @classname ResetPwdReq
 * @since 2023-07-25 15:18
 */
@Data
public class ResetPwdReq {
    /**
     * 新密码
     */
    @NotBlank(message = "密码不能为空")
    @Password
    private String newPassword;

    @NotBlank(message = "token不能为空")
    @Size(max = 512, message = "token长度超过系统限制")
    private String token;
}
