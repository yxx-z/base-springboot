package com.yxx.security.validation;

import com.yxx.security.properties.PasswordPolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 可在 Bean Validation 和非 Web 初始化流程中复用的密码策略检查器。
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicyChecker {

    private final PasswordPolicyProperties properties;

    /**
     * @param value 待检查密码
     * @param enforcePolicy true-检查完整新密码策略；false-只检查 BCrypt 字节上限
     */
    public boolean isValid(String value, boolean enforcePolicy) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > properties.getMaxBytes()) {
            return false;
        }
        if (!enforcePolicy) {
            return true;
        }
        if (value.length() < properties.getMinLength()) {
            return false;
        }
        if (!properties.isAllowWhitespace() && value.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        if (properties.isRequireUppercase() && value.chars().noneMatch(Character::isUpperCase)) {
            return false;
        }
        if (properties.isRequireLowercase() && value.chars().noneMatch(Character::isLowerCase)) {
            return false;
        }
        if (properties.isRequireDigit() && value.chars().noneMatch(Character::isDigit)) {
            return false;
        }
        return !properties.isRequireSpecialCharacter()
                || value.chars().anyMatch(character -> !Character.isLetterOrDigit(character));
    }
}
