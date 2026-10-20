package com.vault.dto;

import com.vault.validation.OnCreate;
import com.vault.validation.OnUpdate;
import com.vault.validation.ValidPurchaseDates;
import com.vault.validation.ValidWarrantyPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import java.math.BigDecimal;
import java.time.LocalDate;

@ValidPurchaseDates(groups = {Default.class, OnCreate.class, OnUpdate.class})
public record PurchaseRequest(
        @NotBlank(message = "Product name is required", groups = {Default.class, OnCreate.class, OnUpdate.class})
        String productName,
        String brand,
        String category,
        String serialNumber,
        String storeName,
        @NotNull(message = "Purchase date is required", groups = {Default.class, OnCreate.class, OnUpdate.class})
        LocalDate purchaseDate,
        @NotNull(message = "Price is required", groups = {Default.class, OnCreate.class, OnUpdate.class})
        @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative", groups = {Default.class, OnCreate.class, OnUpdate.class})
        BigDecimal price,
        @ValidWarrantyPeriod(groups = {Default.class, OnCreate.class, OnUpdate.class})
        int warrantyMonths,
        String supportContact) { }
