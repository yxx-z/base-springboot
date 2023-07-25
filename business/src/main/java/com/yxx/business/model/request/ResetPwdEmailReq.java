package com.yxx.business.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 获取重置密码邮件请求参数
 *
 * @author yxx
 * @classname ResetPwdReq
 * @since 2023-07-25 13:59
 */
@Data
public class ResetPwdEmailReq {

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]+([_.][A-Za-z0-9]+)*@([A-Za-z0-9\\-]+\\.)+[A-Za-z]{2,6}$", message = "请输入正确邮箱号")
    private String email;
}
