package com.yxx.admin.model.request;

import com.yxx.security.validation.Password;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求参数
 *
 * @author yxx
 * @classname EditPwdReq
 * @since 2023-07-25 15:46
 */
@Data
public class EditPwdReq {
    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    @Password(enforcePolicy = false, message = "旧密码长度超过系统限制")
    private String password;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Password
    private String newPassword;
}
