package com.vault.dto;

import java.util.Map;

public record DashboardResponse(long active, long expiring, long expired, long openClaims,
                                Map<String, Long> claimsByStatus, Map<String, Long> purchasesByCategory) {
    public DashboardResponse(long active, long expiring, long expired, long openClaims) {
        this(active, expiring, expired, openClaims, Map.of(), Map.of());
    }
}
