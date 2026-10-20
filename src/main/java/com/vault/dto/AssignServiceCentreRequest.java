package com.vault.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignServiceCentreRequest(@NotNull @Positive Long serviceCentreId) { }
