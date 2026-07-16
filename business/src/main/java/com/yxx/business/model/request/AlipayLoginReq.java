package com.yxx.business.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 支付宝授权登录请求。
 *
 * @param authCode 支付宝客户端取得的一次性授权码
 */
public record AlipayLoginReq(
        @NotBlank(message = "支付宝授权码不能为空")
        @Size(max = 512, message = "支付宝授权码长度超过系统限制")
        String authCode) {
}
