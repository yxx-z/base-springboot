package com.yxx.business.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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
    @Pattern(regexp = "^[A-Za-z0-9]+([_.][A-Za-z0-9]+)*@((qq|163|gmail|88|email)+\\.)+[A-Za-z]{2,6}$", message = "请输入正确邮箱号,目前仅支持 qq邮箱、163邮箱、谷歌邮箱等常用邮箱")
    private String email;
}
