package com.vault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required") @Size(max = 120, message = "Name must be at most 120 characters") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 190, message = "Email must be at most 190 characters") String email,
        @NotBlank(message = "Password is required") @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters") String password,
        @Size(max = 30, message = "Phone must be at most 30 characters") String phone) { }
