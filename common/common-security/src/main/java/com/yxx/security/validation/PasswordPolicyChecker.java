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
        // 非空约束通常由 @NotBlank 表达，但初始化等非 Web 调用也必须得到安全结果。
        if (value == null || value.isBlank()) {
            return false;
        }
        // BCrypt 只处理前 72 字节；按 UTF-8 字节校验可正确覆盖中文和 Emoji。
        if (value.getBytes(StandardCharsets.UTF_8).length > properties.getMaxBytes()) {
            return false;
        }
        if (!enforcePolicy) {
            // 旧密码验证不要求符合当前新密码规则，否则策略升级后用户将无法修改密码。
            return true;
        }
        // 以下规则完全由配置驱动，基础框架使用方可按项目安全等级调整。
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
        // 特殊字符定义为非字母且非数字；空白是否允许由前置规则独立控制。
        return !properties.isRequireSpecialCharacter()
                || value.chars().anyMatch(character -> !Character.isLetterOrDigit(character));
    }
}
