package com.vault.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vault.dto.PurchaseRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PurchaseValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() { validator = Validation.buildDefaultValidatorFactory().getValidator(); }

    @Test
    void acceptsValidPurchaseAcrossSupportedGroups() {
        PurchaseRequest request = request(LocalDate.now().minusDays(1), 12);
        assertTrue(validator.validate(request, Default.class, OnCreate.class, OnUpdate.class).isEmpty());
    }

    @Test
    void rejectsWarrantyOutsideAllowedRange() {
        assertTrue(validator.validate(request(LocalDate.now(), 0), OnCreate.class).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("warrantyMonths")));
        assertTrue(validator.validate(request(LocalDate.now(), 121), OnUpdate.class).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("warrantyMonths")));
    }

    @Test
    void mapsInvalidPurchaseDatesToPurchaseDateField() {
        PurchaseRequest tooOld = request(LocalDate.of(1999, 12, 31), 12);
        PurchaseRequest future = request(LocalDate.now().plusDays(1), 12);
        assertTrue(validator.validate(tooOld).stream().anyMatch(v -> v.getPropertyPath().toString().equals("purchaseDate")));
        assertTrue(validator.validate(future).stream().anyMatch(v -> v.getPropertyPath().toString().equals("purchaseDate")));
    }

    @Test
    void keepsZeroPriceValidAndRejectsNegativePrice() {
        PurchaseRequest zeroPrice = new PurchaseRequest("Thing", null, null, null, null,
                LocalDate.now(), BigDecimal.ZERO, 1, null);
        PurchaseRequest negativePrice = new PurchaseRequest("Thing", null, null, null, null,
                LocalDate.now(), new BigDecimal("-1"), 1, null);
        assertFalse(validator.validate(zeroPrice).stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
        assertTrue(validator.validate(negativePrice).stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    private PurchaseRequest request(LocalDate date, int months) {
        return new PurchaseRequest("Laptop", "Lenovo", "Electronics", "SN-1", "Store",
                date, new BigDecimal("99.00"), months, null);
    }
}
