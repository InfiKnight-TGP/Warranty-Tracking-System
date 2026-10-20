package com.vault.dto;

import com.vault.entity.UserRole;
import java.time.LocalDateTime;

public record UserResponse(Long id, String name, String email, String phone, UserRole role, LocalDateTime createdAt) { }
