package com.yxx.business.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 支付宝 OAuth 授权登录配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "ali")
public class AlipayProperties {

    /** 应用编号。 */
    private String appId;
    /** 商户私钥。 */
    private String merchantPrivateKey;
    /** 字符编码。 */
    private String charset;
    /** 支付宝公钥。 */
    private String alipayPublicKey;
    /** 签名算法。 */
    private String signType;
    /** 支付宝网关地址。 */
    private String serverUrl;
    /** 响应格式。 */
    private String format;
}
