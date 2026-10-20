package com.vault.repository;

import com.vault.entity.Purchase;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {
    List<Purchase> findByOwnerId(Long ownerId);
    List<PurchaseSummary> findSummariesByOwnerId(Long ownerId);
    List<PurchaseSummary> findAllProjectedBy();
    Optional<Purchase> findFirstBySerialNumberIgnoreCase(String serialNumber);
    @Query(name = "Purchase.findExpiringBetween")
    List<Purchase> findExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    @Query(value = "SELECT COALESCE(category, 'Uncategorized') AS category, COUNT(*) AS cnt FROM purchases GROUP BY COALESCE(category, 'Uncategorized')", nativeQuery = true)
    List<Object[]> countPurchasesPerCategoryNative();
    List<Purchase> findByWarrantyExpiryBetweenAndReminder30SentFalse(LocalDate start, LocalDate end);
    List<Purchase> findByWarrantyExpiryBetweenAndReminder7SentFalse(LocalDate start, LocalDate end);
}
