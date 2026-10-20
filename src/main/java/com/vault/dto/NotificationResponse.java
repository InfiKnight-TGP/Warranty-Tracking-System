package com.vault.dto;

import com.vault.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, Long purchaseId, String message,
        NotificationType type, boolean read, LocalDateTime createdAt) { }
