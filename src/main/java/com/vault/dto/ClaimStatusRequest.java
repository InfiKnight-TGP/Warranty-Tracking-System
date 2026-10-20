package com.vault.dto;

import com.vault.entity.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClaimStatusRequest(
        @NotNull(message = "New status is required") ClaimStatus newStatus,
        @Size(max = 1000, message = "Note must be at most 1000 characters") String note) { }
