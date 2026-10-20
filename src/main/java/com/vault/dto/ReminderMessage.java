package com.vault.dto;

import com.vault.entity.NotificationType;
import java.io.Serializable;

public record ReminderMessage(Long purchaseId, Long userId, NotificationType type) implements Serializable {
    private static final long serialVersionUID = 1L;
}
