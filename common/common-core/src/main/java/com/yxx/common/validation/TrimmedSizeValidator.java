package com.yxx.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** {@link TrimmedSize} 的无状态校验实现。 */
public class TrimmedSizeValidator implements ConstraintValidator<TrimmedSize, CharSequence> {

    private int min;
    private int max;

    @Override
    public void initialize(TrimmedSize annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        // 空值职责交给 @NotBlank/@NotNull，便于字段按是否必填组合约束。
        if (value == null) {
            return true;
        }
        int length = value.toString().trim().length();
        return length >= min && length <= max;
    }
}
