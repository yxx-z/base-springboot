package com.yxx.business.model.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

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
    @Length(min = 8, max = 20, message = "密码应为8-20位")
    private String newPassword;

    @NotBlank(message = "token不能为空")
    private String token;
}
