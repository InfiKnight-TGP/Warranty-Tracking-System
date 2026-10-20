package com.vault.service;

import com.vault.dto.NotificationResponse;
import com.vault.entity.Notification;
import com.vault.entity.User;
import com.vault.exception.AccessDeniedException;
import com.vault.exception.ResourceNotFoundException;
import com.vault.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final PurchaseService purchaseService;

    public NotificationService(NotificationRepository notificationRepository, PurchaseService purchaseService) {
        this.notificationRepository = notificationRepository;
        this.purchaseService = purchaseService;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findMine(String email) {
        User user = purchaseService.user(email);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public NotificationResponse markRead(String email, Long id) {
        User user = purchaseService.user(email);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification " + id + " was not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have access to this notification");
        }
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(),
                notification.getPurchase() == null ? null : notification.getPurchase().getId(),
                notification.getMessage(), notification.getType(), notification.isRead(), notification.getCreatedAt());
    }
}
