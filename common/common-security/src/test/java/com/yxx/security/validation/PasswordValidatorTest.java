package com.yxx.security.validation;

import com.yxx.security.properties.PasswordPolicyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 配置化密码策略测试。 */
class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        PasswordPolicyProperties properties = new PasswordPolicyProperties();
        validator = new PasswordValidator(new PasswordPolicyChecker(properties));
        validator.initialize(annotation(true));
    }

    @Test
    void shouldValidateDefaultPasswordPolicy() {
        assertTrue(validator.isValid("Framework2026", null));
        assertFalse(validator.isValid("short1A", null));
        assertFalse(validator.isValid("framework2026", null));
        assertFalse(validator.isValid("Framework Password2026", null));
    }

    @Test
    void shouldOnlyCheckBcryptByteLimitForExistingCredential() {
        validator.initialize(annotation(false));
        assertTrue(validator.isValid("legacy", null));
        assertFalse(validator.isValid("中".repeat(25), null));
    }

    private Password annotation(boolean enforcePolicy) {
        return new Password() {
            @Override
            public boolean enforcePolicy() {
                return enforcePolicy;
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Password.class;
            }
        };
    }
}
