package com.yxx.business.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 支付宝 OAuth 授权登录配置。 */
@Data
@Validated
@ConfigurationProperties(prefix = "ali")
public class AlipayProperties {

    /** 应用编号。 */
    @NotBlank(message = "支付宝应用编号不能为空")
    private String appId;
    /** 商户私钥。 */
    @NotBlank(message = "支付宝商户私钥不能为空")
    private String merchantPrivateKey;
    /** 字符编码。 */
    @NotBlank(message = "支付宝字符编码不能为空")
    private String charset;
    /** 支付宝公钥。 */
    @NotBlank(message = "支付宝公钥不能为空")
    private String alipayPublicKey;
    /** 签名算法。 */
    @NotBlank(message = "支付宝签名算法不能为空")
    private String signType;
    /** 支付宝网关地址。 */
    @NotBlank(message = "支付宝网关地址不能为空")
    private String serverUrl;
    /** 响应格式。 */
    @NotBlank(message = "支付宝响应格式不能为空")
    private String format;
}
