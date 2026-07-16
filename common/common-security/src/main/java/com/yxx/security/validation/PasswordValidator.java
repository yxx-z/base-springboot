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
        this.enforcePolicy = annotation.enforcePolicy();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return policyChecker.isValid(value, enforcePolicy);
    }
}
