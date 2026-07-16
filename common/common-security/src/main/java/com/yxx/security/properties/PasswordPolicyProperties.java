package com.yxx.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户密码策略配置。
 *
 * <p>BCrypt 只处理前 72 个字节，因此最大长度按 UTF-8 字节数校验，而不是简单按 Java
 * 字符数校验。各业务项目可以通过配置调整复杂度，但不应绕过最大字节数限制。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.password-policy")
public class PasswordPolicyProperties {

    /** 密码最少字符数。 */
    private int minLength = 12;

    /** BCrypt 可安全处理的最大 UTF-8 字节数。 */
    private int maxBytes = 72;

    /** 是否至少包含一个大写英文字母。 */
    private boolean requireUppercase = true;

    /** 是否至少包含一个小写英文字母。 */
    private boolean requireLowercase = true;

    /** 是否至少包含一个数字。 */
    private boolean requireDigit = true;

    /** 是否至少包含一个非字母数字字符。 */
    private boolean requireSpecialCharacter = false;

    /** 是否允许密码包含空白字符。 */
    private boolean allowWhitespace = false;
}
