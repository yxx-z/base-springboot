package com.yxx.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里配置
 *
 * @author yxx
 * @classname AliProperties
 * @since 2023-09-14 10:23
 */
@Data
@Component
@ConfigurationProperties(prefix = "ali")
public class AliProperties {
    /**
     * 小程序appId
     */
    private String appId;

    /**
     * 商户私钥
     */
    private String merchantPrivateKey;

    /**
     * 商户公钥
     */
    private String merchantPublicKey;

    /**
     * 编码格式
     */
    private String charset;

    /**
     * 支付宝公钥
     */
    private String alipayPublicKey;

    /**
     * 加密类型
     */
    private String signType;

    /**
     * 支付宝网关
     */
    private String serverUrl;

    /**
     * 异步回调地址
     */
    private String notifyUrl;

    /**
     * 同步回调地址
     */
    private String returnUrl;

    /**
     * 返回格式
     */
    private String format;
}
