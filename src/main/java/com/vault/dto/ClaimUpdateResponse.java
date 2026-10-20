package com.vault.dto;

import com.vault.entity.ClaimStatus;
import java.time.LocalDateTime;

public record ClaimUpdateResponse(Long id, ClaimStatus oldStatus, ClaimStatus newStatus,
        String note, Long updatedById, String updatedByName, LocalDateTime updatedAt) { }
