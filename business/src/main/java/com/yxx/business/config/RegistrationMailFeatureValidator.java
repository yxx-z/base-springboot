package com.yxx.business.config;

import com.yxx.common.properties.MailProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** business 注册验证码邮件启用时的专属配置校验。 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "features.mail", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class RegistrationMailFeatureValidator {

    private final MailProperties properties;

    @PostConstruct
    public void validate() {
        if (properties.getRegisterContent() == null || properties.getRegisterContent().isBlank()) {
            throw new IllegalStateException("邮件功能已启用但缺少配置：mail.register-content");
        }
        if (properties.getRegisterTime() == null || properties.getRegisterTime() <= 0) {
            throw new IllegalStateException("邮件功能配置必须大于0：mail.register-time");
        }
        if (properties.getRegisterMax() == null || properties.getRegisterMax() <= 0) {
            throw new IllegalStateException("邮件功能配置必须大于0：mail.register-max");
        }
    }
}
