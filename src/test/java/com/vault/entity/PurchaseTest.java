package com.vault.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PurchaseTest {
    @Test
    void calculatesWarrantyExpiryFromPurchaseDateAndWarrantyMonths() {
        Purchase purchase = new Purchase("Laptop", "Lenovo", "Electronics", "L-1", "Store",
                LocalDate.of(2025, 1, 15), null, 24, null,
                new User("Demo", "demo@example.com", "encoded", null, UserRole.USER));

        assertEquals(LocalDate.of(2027, 1, 15), purchase.getWarrantyExpiry());
    }

    @Test
    void recalculatesExpiryWhenPurchaseDateOrWarrantyChanges() {
        Purchase purchase = new Purchase("Laptop", "Lenovo", "Electronics", "L-1", "Store",
                LocalDate.of(2025, 1, 15), null, 12, null,
                new User("Demo", "demo@example.com", "encoded", null, UserRole.USER));

        purchase.setPurchaseDate(LocalDate.of(2025, 2, 1));
        purchase.setWarrantyMonths(18);

        assertEquals(LocalDate.of(2026, 8, 1), purchase.getWarrantyExpiry());
    }
}
