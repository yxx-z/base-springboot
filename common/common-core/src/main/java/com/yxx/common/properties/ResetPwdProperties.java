package com.yxx.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 找回密码配置文件
 *
 * @author yxx
 * @classname ResetPwdProperties
 * @since 2023-07-26 10:56
 */
@Component
@Data
@ConfigurationProperties(prefix = "reset-password")
public class ResetPwdProperties {

    /**
     * # 找回密码页面url
     */
    private String basePath;

    /**
     * 每日找回密码最大次数 (防止恶意发送邮件)
     */
    private Integer maxNumber;

    /**
     * 找回密码邮件正文
     */
    private String resetPwdContent;
}
