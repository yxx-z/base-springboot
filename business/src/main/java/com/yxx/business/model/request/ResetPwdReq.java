package com.yxx.business.model.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

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
    @NotEmpty(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码应为8-20位")
    private String newPassword;

    @NotEmpty(message = "token不能为空")
    private String token;
}
