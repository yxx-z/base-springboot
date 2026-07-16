package com.yxx.business.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.yxx.business.properties.AlipayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 支付宝 OAuth 客户端配置。
 *
 * <p>基础框架仅保留支付宝授权登录能力，不提供脱离订单领域的支付、退款和回调示例。</p>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AlipayProperties.class)
@ConditionalOnProperty(prefix = "features.alipay-login", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class AlipayOAuthConfig {

    private final AlipayProperties properties;

    /** 创建用于授权码换取支付宝用户标识的客户端。 */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                properties.getServerUrl(),
                properties.getAppId(),
                properties.getMerchantPrivateKey(),
                properties.getFormat(),
                properties.getCharset(),
                properties.getAlipayPublicKey(),
                properties.getSignType());
    }
}
