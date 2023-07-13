package com.yxx.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author yxx
 * @since 2022-11-12 02:11
 */
@Component
@Data
@ConfigurationProperties(prefix = "mail")
public class MailProperties {
    /**
     * 发件人邮箱
     */
    private String from;

    /**
     * 发件人昵称
     */
    private String fromName;

    /**
     * 主题
     */
    private String subject;
}
