package com.yxx.business.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付宝获取userId请求参数
 *
 * @author yxx
 * @classname AliGetUserIdReq
 * @since 2023-09-14 10:20
 */
@Data
public class AliGetUserIdReq {
    /**
     * 授权码
     */
    @NotBlank(message = "授权码不能为空")
    private String authCode;
}
