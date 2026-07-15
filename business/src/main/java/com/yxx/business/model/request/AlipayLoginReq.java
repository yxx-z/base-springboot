package com.yxx.business.model.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 支付宝授权登录请求。
 *
 * @param authCode 支付宝客户端取得的一次性授权码
 */
public record AlipayLoginReq(
        @NotBlank(message = "支付宝授权码不能为空") String authCode) {
}
