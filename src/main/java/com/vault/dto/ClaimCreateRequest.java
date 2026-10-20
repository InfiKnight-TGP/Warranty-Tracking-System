package com.vault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimCreateRequest(
        @NotBlank(message = "Issue description is required")
        @Size(max = 2000, message = "Issue description must be at most 2000 characters") String issueDescription) { }
