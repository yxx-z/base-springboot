package com.yxx.security.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 根据配置执行密码策略校验。 */
@Component
@RequiredArgsConstructor
public class PasswordValidator implements ConstraintValidator<Password, String> {

    private final PasswordPolicyChecker policyChecker;
    private boolean enforcePolicy;

    @Override
    public void initialize(Password annotation) {
        // 每个约束声明可以选择完整新密码策略或仅检查编码器安全上限。
        this.enforcePolicy = annotation.enforcePolicy();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // 空值交给 @NotBlank 等职责明确的约束处理，避免重复产生两条校验错误。
            return true;
        }
        return policyChecker.isValid(value, enforcePolicy);
    }
}
