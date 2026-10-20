package com.vault.controller;

import com.vault.dto.AssignServiceCentreRequest;
import com.vault.dto.BatchExecutionResponse;
import com.vault.dto.ClaimResponse;
import com.vault.dto.DashboardResponse;
import com.vault.dto.UserResponse;
import com.vault.service.AdminService;
import com.vault.service.BatchJobService;
import com.vault.service.ClaimService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final ClaimService claimService;
    private final BatchJobService batchJobService;

    public AdminController(AdminService adminService, ClaimService claimService, BatchJobService batchJobService) {
        this.adminService = adminService;
        this.claimService = claimService;
        this.batchJobService = batchJobService;
    }

    @GetMapping("/users")
    public List<UserResponse> users(Principal principal) { return adminService.users(principal.getName()); }

    @GetMapping("/claims")
    public List<ClaimResponse> claims(Principal principal) { return claimService.findAllForAdmin(principal.getName()); }

    @PutMapping("/claims/{id}/assign")
    public ClaimResponse assign(Principal principal, @PathVariable Long id,
                                @Valid @RequestBody AssignServiceCentreRequest request) {
        return claimService.assign(principal.getName(), id, request.serviceCentreId());
    }

    @GetMapping("/stats")
    public DashboardResponse stats(Principal principal) { return adminService.dashboard(principal.getName()); }

    @PostMapping("/batch/reminders/run")
    public BatchExecutionResponse runRemindersBatch(Principal principal) {
        return batchJobService.runReminderJobForAdmin(principal.getName());
    }

    @PostMapping("/batch/export/run")
    public ResponseEntity<Resource> runExportBatch(Principal principal) {
        Path csv = batchJobService.runExportJobForAdmin(principal.getName());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + csv.getFileName() + "\"")
                .body(new FileSystemResource(csv));
    }

    @GetMapping("/batch/executions")
    public List<BatchExecutionResponse> listBatchExecutions(Principal principal) {
        return batchJobService.listExecutionsForAdmin(principal.getName());
    }
}
