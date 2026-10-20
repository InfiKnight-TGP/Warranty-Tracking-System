package com.vault.dto;

import com.vault.entity.ClaimStatus;
import java.time.LocalDateTime;

public record ClaimResponse(Long id, String issueDescription, ClaimStatus status,
        Long purchaseId, String productName, Long raisedById, String raisedByName,
        Long serviceCentreId, String serviceCentreName, LocalDateTime createdAt, LocalDateTime updatedAt) { }
