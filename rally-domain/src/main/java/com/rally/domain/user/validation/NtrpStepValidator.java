package com.rally.domain.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class NtrpStepValidator implements ConstraintValidator<NtrpStep, BigDecimal> {

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.multiply(new BigDecimal("2"))
                .remainder(new BigDecimal("1"))
                .compareTo(BigDecimal.ZERO) == 0;
    }
}
