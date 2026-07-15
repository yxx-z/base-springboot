package com.yxx.business.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝业务配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ali")
public class AliProperties {

    /** 应用编号。 */
    private String appId;
    /** 商户私钥。 */
    private String merchantPrivateKey;
    /** 商户公钥。 */
    private String merchantPublicKey;
    /** 字符编码。 */
    private String charset;
    /** 支付宝公钥。 */
    private String alipayPublicKey;
    /** 签名算法。 */
    private String signType;
    /** 支付宝网关地址。 */
    private String serverUrl;
    /** 异步回调地址。 */
    private String notifyUrl;
    /** 同步回调地址。 */
    private String returnUrl;
    /** 响应格式。 */
    private String format;
}
