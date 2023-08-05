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
     * 注册验证码过期时间
     */
    private Integer registerTime;

    /**
     * 一个邮箱每日最大注册次数 (防止恶意发送邮件)
     */
    private Integer registerMax;

    /**
     * 注册邮件正文
     */
    private String registerContent;

    /**
     * ip异常邮件正文
     */
    private String ipUnusualContent;
}
