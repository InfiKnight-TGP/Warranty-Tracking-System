package com.vault.controller;

import com.vault.dto.ClaimCreateRequest;
import com.vault.dto.ClaimResponse;
import com.vault.dto.ClaimStatusRequest;
import com.vault.dto.ClaimUpdateResponse;
import com.vault.service.ClaimService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClaimController {
    private final ClaimService claimService;
    public ClaimController(ClaimService claimService) { this.claimService = claimService; }

    @PostMapping("/api/purchases/{id}/claims")
    public ResponseEntity<ClaimResponse> raise(Principal principal, @PathVariable Long id,
            @Valid @RequestBody ClaimCreateRequest request) {
        return ResponseEntity.status(201).body(claimService.raise(principal.getName(), id, request.issueDescription()));
    }

    @GetMapping("/api/claims/my")
    public List<ClaimResponse> mine(Principal principal) { return claimService.findMine(principal.getName()); }

    @GetMapping("/api/claims/assigned")
    public List<ClaimResponse> assigned(Principal principal) { return claimService.findAssigned(principal.getName()); }

    @PutMapping("/api/claims/{id}/status")
    public ClaimResponse updateStatus(Principal principal, @PathVariable Long id,
            @Valid @RequestBody ClaimStatusRequest request) {
        return claimService.updateStatus(principal.getName(), id, request.newStatus(), request.note());
    }

    @GetMapping("/api/claims/{id}/history")
    public List<ClaimUpdateResponse> history(Principal principal, @PathVariable Long id) {
        return claimService.history(principal.getName(), id);
    }
}
