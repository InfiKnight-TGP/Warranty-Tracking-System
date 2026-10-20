package com.vault.controller;

import com.vault.dto.NotificationResponse;
import com.vault.service.NotificationService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) { this.notificationService = notificationService; }

    @GetMapping
    public List<NotificationResponse> mine(Principal principal) { return notificationService.findMine(principal.getName()); }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(Principal principal, @PathVariable Long id) {
        return notificationService.markRead(principal.getName(), id);
    }
}
