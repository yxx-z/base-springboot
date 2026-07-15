package com.yxx.business.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.yxx.business.properties.AliProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝支付配置。
 *
 * <p>支付能力属于用户端业务集成，不放入公共基础模块，避免管理端被迫加载支付宝 SDK。</p>
 */
@Configuration
@RequiredArgsConstructor
public class AliPayConfig {

    private final AliProperties aliProperties;

    /**
     * 创建手机网页支付请求模板。
     *
     * @return 手机网页支付请求
     */
    public AlipayTradeWapPayRequest alipayTradeWapPayRequest() {
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setNotifyUrl(aliProperties.getNotifyUrl());
        request.setReturnUrl(aliProperties.getReturnUrl());
        return request;
    }

    /**
     * 创建电脑网页支付请求模板。
     *
     * @return 电脑网页支付请求
     */
    public AlipayTradePagePayRequest alipayTradePagePayRequest() {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(aliProperties.getNotifyUrl());
        request.setReturnUrl(aliProperties.getReturnUrl());
        return request;
    }

    /**
     * 创建支付宝客户端。
     *
     * @return 支付宝客户端
     */
    @Bean
    public AlipayClient defaultAlipayClient() {
        return new DefaultAlipayClient(
                aliProperties.getServerUrl(),
                aliProperties.getAppId(),
                aliProperties.getMerchantPrivateKey(),
                aliProperties.getFormat(),
                aliProperties.getCharset(),
                aliProperties.getAlipayPublicKey(),
                aliProperties.getSignType());
    }
}
