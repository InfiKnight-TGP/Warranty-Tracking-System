package com.vault.repository;

import com.vault.entity.Purchase;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class PurchaseSpecifications {
    private PurchaseSpecifications() { }

    public static Specification<Purchase> withFilters(Long ownerId, String keyword, String category, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ownerId != null) predicates.add(cb.equal(root.get("owner").get("id"), ownerId));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("productName")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("brand"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("serialNumber"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("storeName"), "")), pattern)));
            }
            if (category != null && !category.isBlank()) {
                String pattern = "%" + category.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("category"), "")), pattern));
            }
            if (status != null && !status.isBlank()) {
                LocalDate today = LocalDate.now();
                switch (status.trim().toUpperCase(Locale.ROOT)) {
                    case "EXPIRED" -> predicates.add(cb.lessThan(root.get("warrantyExpiry"), today));
                    case "EXPIRING" -> predicates.add(cb.between(root.get("warrantyExpiry"), today, today.plusDays(30)));
                    case "ACTIVE" -> predicates.add(cb.greaterThan(root.get("warrantyExpiry"), today.plusDays(30)));
                    default -> throw new IllegalArgumentException("Status must be ACTIVE, EXPIRING, or EXPIRED");
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
