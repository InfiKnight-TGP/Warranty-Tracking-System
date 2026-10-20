package com.vault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseResponse(Long id, String productName, String brand, String category,
        String serialNumber, String storeName, LocalDate purchaseDate, BigDecimal price,
        int warrantyMonths, LocalDate warrantyExpiry, String billFilePath, String supportContact,
        Long ownerId, LocalDateTime createdAt, long daysLeft, String status) { }
