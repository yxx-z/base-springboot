package com.yxx.business.model.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

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
    @Length(min = 8, max = 15, message = "旧密码的长度为8-20位")
    private String password;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Length(min = 8, max = 15, message = "新密码的长度应为8-20位")
    private String newPassword;
}
