package com.vault.validation;

import com.vault.dto.PurchaseRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class PurchaseDatesValidator implements ConstraintValidator<ValidPurchaseDates, PurchaseRequest> {
    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);

    @Override
    public boolean isValid(PurchaseRequest request, ConstraintValidatorContext context) {
        if (request == null || request.purchaseDate() == null) return true;
        LocalDate date = request.purchaseDate();
        LocalDate today = LocalDate.now();
        if (!date.isBefore(MIN_DATE) && !date.isAfter(today)) return true;

        String message = date.isAfter(today)
                ? "Purchase date cannot be in the future"
                : "Purchase date cannot be before 2000-01-01";
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("purchaseDate")
                .addConstraintViolation();
        return false;
    }
}
