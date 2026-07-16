package com.yxx.framework.config.feature;

import com.yxx.common.properties.MailProperties;
import com.yxx.common.properties.MyWebProperties;
import com.yxx.common.properties.ResetPwdProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 邮件能力启用时的启动期配置校验器。
 *
 * <p>邮件能力关闭时该 Bean 不创建，也不会要求项目提供模板和发件人配置；启用后则在应用
 * 接收请求前一次性校验，避免运行到验证码或重置密码流程才出现空指针。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "features.mail", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class MailFeatureValidator {

    private final MailProperties mailProperties;
    private final ResetPwdProperties resetPwdProperties;
    private final MyWebProperties webProperties;

    @PostConstruct
    public void validate() {
        requireText(mailProperties.getFrom(), "mail.from");
        requireText(mailProperties.getFromName(), "mail.from-name");
        requireText(mailProperties.getIpUnusualContent(), "mail.ip-unusual-content");
        requireText(resetPwdProperties.getBasePath(), "reset-password.base-path");
        requireText(resetPwdProperties.getResetPwdContent(), "reset-password.reset-pwd-content");
        requirePositive(resetPwdProperties.getMaxNumber(), "reset-password.max-number");
        requirePositive(resetPwdProperties.getResetPwdTime(), "reset-password.reset-pwd-time");
        requireText(webProperties.getDomain(), "web.domain");
    }

    private void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("邮件功能已启用但缺少配置：" + propertyName);
        }
    }

    private void requirePositive(Integer value, String propertyName) {
        if (value == null || value <= 0) {
            throw new IllegalStateException("邮件功能配置必须大于0：" + propertyName);
        }
    }
}
