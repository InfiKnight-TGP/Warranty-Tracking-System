package com.vault.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CLAIM")
public class ClaimNotification extends Notification {
    protected ClaimNotification() { }
    public ClaimNotification(User user, Purchase purchase, String message) {
        super(user, purchase, message, NotificationType.CLAIM_UPDATE);
    }
}
