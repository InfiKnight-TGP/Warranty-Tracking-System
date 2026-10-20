package com.vault.messaging;

import com.vault.dto.ClaimUpdateMessage;
import com.vault.entity.Claim;
import com.vault.entity.ClaimNotification;
import com.vault.exception.ResourceNotFoundException;
import com.vault.repository.ClaimRepository;
import com.vault.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClaimUpdateListener {
    private static final Logger logger = LoggerFactory.getLogger(ClaimUpdateListener.class);
    private final ClaimRepository claimRepository;
    private final NotificationRepository notificationRepository;

    public ClaimUpdateListener(ClaimRepository claimRepository, NotificationRepository notificationRepository) {
        this.claimRepository = claimRepository;
        this.notificationRepository = notificationRepository;
    }

    @JmsListener(destination = "claim.updates")
    @Transactional
    public void receive(ClaimUpdateMessage message) {
        Claim claim = claimRepository.findById(message.claimId())
                .orElseThrow(() -> new ResourceNotFoundException("Claim " + message.claimId() + " was not found"));
        String text = "Claim " + claim.getId() + " for " + claim.getPurchase().getProductName()
                + " was updated to " + message.newStatus() + ".";
        if (message.note() != null && !message.note().isBlank()) text += " Note: " + message.note();
        notificationRepository.save(new ClaimNotification(claim.getRaisedBy(), claim.getPurchase(), text));
        logger.info("Created claim update notification for claim {}", claim.getId());
    }
}
