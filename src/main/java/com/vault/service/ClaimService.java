package com.vault.service;

import com.vault.dto.ClaimResponse;
import com.vault.dto.ClaimUpdateResponse;
import com.vault.entity.Claim;
import com.vault.entity.ClaimStatus;
import com.vault.entity.ClaimUpdate;
import com.vault.entity.Purchase;
import com.vault.entity.User;
import com.vault.entity.UserRole;
import com.vault.exception.AccessDeniedException;
import com.vault.exception.InvalidStatusTransitionException;
import com.vault.exception.ResourceNotFoundException;
import com.vault.exception.WarrantyExpiredException;
import com.vault.repository.ClaimRepository;
import com.vault.repository.ClaimUpdateRepository;
import com.vault.repository.UserRepository;
import com.vault.messaging.ClaimUpdateProducer;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimService {
    private final ClaimRepository claimRepository;
    private final ClaimUpdateRepository updateRepository;
    private final PurchaseService purchaseService;
    private final UserRepository userRepository;
    private final ClaimUpdateProducer claimUpdateProducer;

    public ClaimService(ClaimRepository claimRepository, ClaimUpdateRepository updateRepository,
                        PurchaseService purchaseService, UserRepository userRepository,
                        ClaimUpdateProducer claimUpdateProducer) {
        this.claimRepository = claimRepository;
        this.updateRepository = updateRepository;
        this.purchaseService = purchaseService;
        this.userRepository = userRepository;
        this.claimUpdateProducer = claimUpdateProducer;
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ClaimResponse raise(String email, Long purchaseId, String issueDescription) {
        User user = purchaseService.user(email);
        Purchase purchase = purchaseService.ownedPurchase(email, purchaseId);
        if (purchase.getWarrantyExpiry().isBefore(LocalDate.now())) {
            throw new WarrantyExpiredException("A claim can only be raised while the warranty is active");
        }
        return toResponse(claimRepository.save(new Claim(issueDescription, purchase, user)));
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> findMine(String email) {
        User user = purchaseService.user(email);
        return claimRepository.findByRaisedById(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> findAssigned(String email) {
        User serviceCentre = requireRole(email, UserRole.SERVICE_CENTER);
        return claimRepository.findByServiceCentreId(serviceCentre.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> findAll() { return claimRepository.findAll().stream().map(this::toResponse).toList(); }

    @Transactional(readOnly = true)
    public List<ClaimResponse> findAllForAdmin(String email) {
        requireRole(email, UserRole.ADMIN);
        return findAll();
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ClaimResponse updateStatus(String email, Long claimId, ClaimStatus newStatus, String note) {
        User serviceCentre = requireRole(email, UserRole.SERVICE_CENTER);
        Claim claim = getClaim(claimId);
        if (claim.getServiceCentre() == null || !claim.getServiceCentre().getId().equals(serviceCentre.getId())) {
            throw new AccessDeniedException("This claim is not assigned to your service centre");
        }
        ClaimStatus oldStatus = claim.getStatus();
        if (!canTransition(oldStatus, newStatus)) {
            throw new InvalidStatusTransitionException("Claim cannot move from " + oldStatus + " to " + newStatus);
        }
        claim.setStatus(newStatus);
        Claim savedClaim = claimRepository.save(claim);
        updateRepository.save(new ClaimUpdate(savedClaim, oldStatus, newStatus, note, serviceCentre));
        claimUpdateProducer.send(savedClaim.getId(), newStatus, note);
        return toResponse(savedClaim);
    }

    @Transactional(readOnly = true)
    public List<ClaimUpdateResponse> history(String email, Long claimId) {
        User user = purchaseService.user(email);
        Claim claim = getClaim(claimId);
        boolean isOwner = claim.getRaisedBy().getId().equals(user.getId());
        boolean isAssignedCentre = claim.getServiceCentre() != null && claim.getServiceCentre().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAssignedCentre && !isAdmin) throw new AccessDeniedException("You do not have access to this claim history");
        return updateRepository.findByClaimIdOrderByUpdatedAtAsc(claimId).stream().map(this::toUpdateResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ClaimResponse assign(String adminEmail, Long claimId, Long serviceCentreId) {
        requireRole(adminEmail, UserRole.ADMIN);
        Claim claim = getClaim(claimId);
        User serviceCentre = userRepository.findById(serviceCentreId)
                .orElseThrow(() -> new ResourceNotFoundException("Service centre " + serviceCentreId + " was not found"));
        if (serviceCentre.getRole() != UserRole.SERVICE_CENTER) {
            throw new IllegalArgumentException("The selected user is not a service centre");
        }
        claim.setServiceCentre(serviceCentre);
        return toResponse(claimRepository.save(claim));
    }

    @Transactional(readOnly = true)
    public long countOpenClaims() {
        return claimRepository.countByStatusNotIn(List.of(ClaimStatus.RETURNED, ClaimStatus.REJECTED));
    }

    @Transactional(readOnly = true)
    public long countOpenClaimsFor(String email) {
        User user = purchaseService.user(email);
        return claimRepository.countByRaisedByIdAndStatusNotIn(user.getId(),
                List.of(ClaimStatus.RETURNED, ClaimStatus.REJECTED));
    }

    @Transactional(readOnly = true)
    public Claim getClaim(Long id) {
        return claimRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Claim " + id + " was not found"));
    }

    public static boolean canTransition(ClaimStatus oldStatus, ClaimStatus newStatus) {
        if (oldStatus == null || newStatus == null) return false;
        return switch (oldStatus) {
            case RAISED -> newStatus == ClaimStatus.RECEIVED || newStatus == ClaimStatus.REJECTED;
            case RECEIVED -> newStatus == ClaimStatus.IN_REPAIR || newStatus == ClaimStatus.REJECTED;
            case IN_REPAIR -> newStatus == ClaimStatus.REPAIRED;
            case REPAIRED -> newStatus == ClaimStatus.RETURNED;
            case RETURNED, REJECTED -> false;
        };
    }

    private User requireRole(String email, UserRole role) {
        User user = purchaseService.user(email);
        if (user.getRole() != role) throw new AccessDeniedException("This action requires the " + role + " role");
        return user;
    }

    private ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getIssueDescription(), claim.getStatus(),
                claim.getPurchase().getId(), claim.getPurchase().getProductName(),
                claim.getRaisedBy().getId(), claim.getRaisedBy().getName(),
                claim.getServiceCentre() == null ? null : claim.getServiceCentre().getId(),
                claim.getServiceCentre() == null ? null : claim.getServiceCentre().getName(),
                claim.getCreatedAt(), claim.getUpdatedAt());
    }

    private ClaimUpdateResponse toUpdateResponse(ClaimUpdate update) {
        return new ClaimUpdateResponse(update.getId(), update.getOldStatus(), update.getNewStatus(),
                update.getNote(), update.getUpdatedBy().getId(), update.getUpdatedBy().getName(), update.getUpdatedAt());
    }
}
