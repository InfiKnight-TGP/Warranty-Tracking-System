package com.vault.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WarrantyPeriodValidator implements ConstraintValidator<ValidWarrantyPeriod, Integer> {
    private int min;
    private int max;

    @Override
    public void initialize(ValidWarrantyPeriod annotation) {
        min = annotation.min();
        max = annotation.max();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value != null && value >= min && value <= max;
    }
}
