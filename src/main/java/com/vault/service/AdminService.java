package com.vault.service;

import com.vault.dto.DashboardResponse;
import com.vault.dto.UserResponse;
import com.vault.entity.User;
import com.vault.entity.UserRole;
import com.vault.exception.AccessDeniedException;
import com.vault.repository.ClaimRepository;
import com.vault.repository.PurchaseRepository;
import com.vault.repository.PurchaseSummary;
import com.vault.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final ClaimRepository claimRepository;
    private final PurchaseService purchaseService;
    private final ClaimService claimService;

    public AdminService(UserRepository userRepository, PurchaseRepository purchaseRepository, ClaimRepository claimRepository,
                        PurchaseService purchaseService, ClaimService claimService) {
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
        this.claimRepository = claimRepository;
        this.purchaseService = purchaseService;
        this.claimService = claimService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> users(String email) {
        requireAdmin(email);
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "'admin:' + #email")
    public DashboardResponse dashboard(String email) {
        requireAdmin(email);
        Map<String, Long> claimsByStatus = aggregate(claimRepository.countClaimsPerStatusNative());
        Map<String, Long> purchasesByCategory = aggregate(purchaseRepository.countPurchasesPerCategoryNative());
        return dashboardCounts(purchaseRepository.findAllProjectedBy(), claimService.countOpenClaims(),
                claimsByStatus, purchasesByCategory);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "'user:' + #email")
    public DashboardResponse dashboardForUser(String email) {
        User user = purchaseService.user(email);
        return dashboardCounts(purchaseRepository.findSummariesByOwnerId(user.getId()),
                claimService.countOpenClaimsFor(email), Map.of(), Map.of());
    }

    public void requireAdmin(String email) {
        User user = purchaseService.user(email);
        if (user.getRole() != UserRole.ADMIN) throw new AccessDeniedException("This action requires the ADMIN role");
    }

    private DashboardResponse dashboardCounts(List<PurchaseSummary> summaries, long openClaims,
                                              Map<String, Long> claimsByStatus, Map<String, Long> purchasesByCategory) {
        long active = summaries.stream().filter(p -> PurchaseService.statusFromExpiry(p.getWarrantyExpiry()).equals("ACTIVE")).count();
        long expiring = summaries.stream().filter(p -> PurchaseService.statusFromExpiry(p.getWarrantyExpiry()).equals("EXPIRING")).count();
        long expired = summaries.stream().filter(p -> PurchaseService.statusFromExpiry(p.getWarrantyExpiry()).equals("EXPIRED")).count();
        return new DashboardResponse(active, expiring, expired, openClaims, claimsByStatus, purchasesByCategory);
    }

    private Map<String, Long> aggregate(List<Object[]> rows) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (Object[] row : rows) values.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        return values;
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole(), user.getCreatedAt());
    }
}
