package com.vault.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PurchaseDatesValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPurchaseDates {
    String message() default "Purchase date must be between 2000-01-01 and today";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
