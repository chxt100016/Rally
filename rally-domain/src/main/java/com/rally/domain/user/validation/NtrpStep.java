package com.rally.domain.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验 NTRP 分值步长为 0.5
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NtrpStepValidator.class)
@Documented
public @interface NtrpStep {

    String message() default "NTRP 分值步长必须为 0.5";


}
