package com.vault.messaging;

import com.vault.dto.ReminderMessage;
import com.vault.entity.ExpiryNotification;
import com.vault.entity.Purchase;
import com.vault.entity.User;
import com.vault.exception.ResourceNotFoundException;
import com.vault.repository.NotificationRepository;
import com.vault.repository.PurchaseRepository;
import com.vault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReminderListener {
    private static final Logger logger = LoggerFactory.getLogger(ReminderListener.class);
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public ReminderListener(PurchaseRepository purchaseRepository, UserRepository userRepository,
                           NotificationRepository notificationRepository) {
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @JmsListener(destination = "warranty.reminders")
    @Transactional
    public void receive(ReminderMessage message) {
        Purchase purchase = purchaseRepository.findById(message.purchaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase " + message.purchaseId() + " was not found"));
        User user = userRepository.findById(message.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User " + message.userId() + " was not found"));
        int days = message.type() == com.vault.entity.NotificationType.EXPIRY_7 ? 7 : 30;
        String text = "Warranty for " + purchase.getProductName() + " expires on " + purchase.getWarrantyExpiry()
                + " (within " + days + " days).";
        notificationRepository.save(new ExpiryNotification(user, purchase, text, message.type()));
        logger.info("Created {} warranty notification for purchase {} and user {}", message.type(), purchase.getId(), user.getId());
    }
}
