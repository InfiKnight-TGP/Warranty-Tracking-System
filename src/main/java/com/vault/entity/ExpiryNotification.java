package com.vault.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("EXPIRY")
public class ExpiryNotification extends Notification {
    protected ExpiryNotification() { }
    public ExpiryNotification(User user, Purchase purchase, String message, NotificationType type) {
        super(user, purchase, message, type);
    }
}
