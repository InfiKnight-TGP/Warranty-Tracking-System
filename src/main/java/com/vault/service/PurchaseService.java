package com.vault.service;

import com.vault.dto.PurchaseRequest;
import com.vault.dto.PurchaseResponse;
import com.vault.entity.Purchase;
import com.vault.entity.User;
import com.vault.exception.AccessDeniedException;
import com.vault.exception.ResourceNotFoundException;
import com.vault.repository.PurchaseRepository;
import com.vault.repository.PurchaseSpecifications;
import com.vault.repository.UserRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;

    public PurchaseService(PurchaseRepository purchaseRepository, UserRepository userRepository) {
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> findMine(String email, String keyword, String category, String status) {
        User owner = user(email);
        return purchaseRepository.findAll(PurchaseSpecifications.withFilters(owner.getId(), keyword, category, status)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getMine(String email, Long id) { return toResponse(ownedPurchase(email, id)); }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PurchaseResponse create(String email, PurchaseRequest request) {
        User owner = user(email);
        Purchase purchase = new Purchase(request.productName(), request.brand(), request.category(),
                request.serialNumber(), request.storeName(), request.purchaseDate(), request.price(),
                request.warrantyMonths(), request.supportContact(), owner);
        return toResponse(purchaseRepository.save(purchase));
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PurchaseResponse update(String email, Long id, PurchaseRequest request) {
        Purchase purchase = ownedPurchase(email, id);
        purchase.setProductName(request.productName());
        purchase.setBrand(request.brand());
        purchase.setCategory(request.category());
        purchase.setSerialNumber(request.serialNumber());
        purchase.setStoreName(request.storeName());
        purchase.setPurchaseDate(request.purchaseDate());
        purchase.setPrice(request.price());
        purchase.setWarrantyMonths(request.warrantyMonths());
        purchase.setSupportContact(request.supportContact());
        return toResponse(purchaseRepository.save(purchase));
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void delete(String email, Long id) { purchaseRepository.delete(ownedPurchase(email, id)); }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PurchaseResponse setBillPath(String email, Long id, String path) {
        Purchase purchase = ownedPurchase(email, id);
        purchase.setBillFilePath(path);
        return toResponse(purchaseRepository.save(purchase));
    }

    @Transactional(readOnly = true)
    public Purchase ownedPurchase(String email, Long id) {
        User owner = user(email);
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase " + id + " was not found"));
        if (!purchase.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You do not have access to this purchase");
        }
        return purchase;
    }

    @Transactional(readOnly = true)
    public Purchase findBySerialNumber(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new ResourceNotFoundException("Serial number is required");
        }
        return purchaseRepository.findFirstBySerialNumberIgnoreCase(serialNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No purchase found for serial number " + serialNumber));
    }

    @Transactional(readOnly = true)
    public List<Purchase> findExpiringBetween(LocalDate start, LocalDate end) {
        return purchaseRepository.findExpiringBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<Purchase> findAll() { return purchaseRepository.findAll(); }

    @Transactional(readOnly = true)
    public PurchaseResponse toResponse(Purchase purchase) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), purchase.getWarrantyExpiry());
        return new PurchaseResponse(purchase.getId(), purchase.getProductName(), purchase.getBrand(),
                purchase.getCategory(), purchase.getSerialNumber(), purchase.getStoreName(),
                purchase.getPurchaseDate(), purchase.getPrice(), purchase.getWarrantyMonths(),
                purchase.getWarrantyExpiry(), purchase.getBillFilePath(), purchase.getSupportContact(),
                purchase.getOwner().getId(), purchase.getCreatedAt(), daysLeft, status(purchase));
    }

    public static String status(Purchase purchase) {
        return statusFromExpiry(purchase.getWarrantyExpiry());
    }

    public static String statusFromExpiry(LocalDate warrantyExpiry) {
        LocalDate today = LocalDate.now();
        if (warrantyExpiry.isBefore(today)) return "EXPIRED";
        if (!warrantyExpiry.isAfter(today.plusDays(30))) return "EXPIRING";
        return "ACTIVE";
    }

    public User user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }

}
