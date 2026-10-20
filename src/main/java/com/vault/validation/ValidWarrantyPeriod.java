package com.vault.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = WarrantyPeriodValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidWarrantyPeriod {
    String message() default "Warranty period must be between 1 and 120 months";
    int min() default 1;
    int max() default 120;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
