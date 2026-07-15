package com.yxx.common.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.yxx.common.properties.AliProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付配置
 *
 * @author yxx
 * @classname AliPayConfig
 * @since 2023-09-15 10:14
 */
@Component
@RequiredArgsConstructor
public class AliPayConfig {

    private final AliProperties aliProperties;

    /**
     * 设置手机网页支付请求基础参数
     *
     * @return {@link AlipayTradeWapPayRequest }
     * @author yxx
     */
    @Bean
    public AlipayTradeWapPayRequest alipayTradeWapPayRequest() {
        AlipayTradeWapPayRequest wapPayRequest = new AlipayTradeWapPayRequest();
        // 异步回调地址
        wapPayRequest.setNotifyUrl(aliProperties.getNotifyUrl());
        // 同步回调地址
        wapPayRequest.setReturnUrl(aliProperties.getReturnUrl());
        return wapPayRequest;
    }

    /**
     * 设置电脑网页支付请求基础参数
     *
     * @return {@link AlipayTradeWapPayRequest }
     * @author yxx
     */
    @Bean
    public AlipayTradePagePayRequest alipayTradePagePayRequest() {
        AlipayTradePagePayRequest webRequest = new AlipayTradePagePayRequest();
        // 异步回调地址
        webRequest.setNotifyUrl(aliProperties.getNotifyUrl());
        // 同步回调地址
        webRequest.setReturnUrl(aliProperties.getReturnUrl());
        return webRequest;
    }

    /**
     * 设置支付宝客户端
     *
     * @return {@link AlipayClient }
     * @author yxx
     */
    @Bean
    public AlipayClient defaultAlipayClient() {
        return new DefaultAlipayClient(aliProperties.getServerUrl(), aliProperties.getAppId(),
                aliProperties.getMerchantPrivateKey(), aliProperties.getFormat(), aliProperties.getCharset(),
                aliProperties.getAlipayPublicKey(), aliProperties.getSignType()
        );
    }
}
