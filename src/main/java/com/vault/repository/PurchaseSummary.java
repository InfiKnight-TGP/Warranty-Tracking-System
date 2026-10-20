package com.vault.repository;

import java.time.LocalDate;

public interface PurchaseSummary {
    Long getId();
    String getProductName();
    LocalDate getWarrantyExpiry();
}
