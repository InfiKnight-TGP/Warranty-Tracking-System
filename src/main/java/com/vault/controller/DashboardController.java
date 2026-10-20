package com.vault.controller;

import com.vault.dto.DashboardResponse;
import com.vault.service.AdminService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
    private final AdminService adminService;
    public DashboardController(AdminService adminService) { this.adminService = adminService; }

    @GetMapping("/api/dashboard")
    public DashboardResponse dashboard(Principal principal) { return adminService.dashboardForUser(principal.getName()); }
}
