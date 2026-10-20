package com.vault.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vault.entity.Purchase;
import com.vault.entity.User;
import com.vault.entity.UserRole;
import com.vault.service.AdminService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:spec-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "vault.scheduler.cron=0 0 0 1 1 *"
})
@Transactional
class PurchaseSpecificationsTest {
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminService adminService;

    @Test
    void filtersOwnerPurchasesByKeywordCategoryAndWarrantyStatus() {
        User owner = userRepository.save(new User("Spec User", "spec@vault.com", "secret", null, UserRole.USER));
        LocalDate today = LocalDate.now();
        purchaseRepository.save(purchase("MacBook Pro", "Apple", "Computers", "APL-991", today.minusMonths(2), 12, owner));
        purchaseRepository.save(purchase("Galaxy S24", "Samsung", "Phones", "SAM-220", today.minusDays(30), 1, owner));
        purchaseRepository.save(purchase("Old Toaster", "Philips", "Kitchen", "PHL-001", today.minusMonths(20), 12, owner));
        purchaseRepository.save(purchase("Other Laptop", "Dell", "Computers", "DE-1", today.minusMonths(2), 12,
                userRepository.findByEmail("user@vault.com").orElseThrow()));

        List<Purchase> byKeyword = purchaseRepository.findAll(
                PurchaseSpecifications.withFilters(owner.getId(), "apl-991", null, null));
        assertEquals(List.of("MacBook Pro"), byKeyword.stream().map(Purchase::getProductName).toList());
        assertEquals(List.of("Galaxy S24"), purchaseRepository.findAll(
                PurchaseSpecifications.withFilters(owner.getId(), null, "phones", "EXPIRING"))
                .stream().map(Purchase::getProductName).toList());
        assertEquals(List.of("Old Toaster"), purchaseRepository.findAll(
                PurchaseSpecifications.withFilters(owner.getId(), null, null, "EXPIRED"))
                .stream().map(Purchase::getProductName).toList());
        assertEquals(List.of("MacBook Pro"), purchaseRepository.findAll(
                PurchaseSpecifications.withFilters(owner.getId(), null, null, "ACTIVE"))
                .stream().map(Purchase::getProductName).toList());
    }

    @Test
    void adminDashboardUsesProjectionAndNativeAggregations() {
        var dashboard = adminService.dashboard("admin@vault.com");
        assertTrue(dashboard.purchasesByCategory().containsKey("Electronics"));
        assertTrue(dashboard.claimsByStatus().isEmpty() || dashboard.claimsByStatus().containsKey("RAISED"));
    }

    private Purchase purchase(String name, String brand, String category, String serial,
                              LocalDate date, int months, User owner) {
        return new Purchase(name, brand, category, serial, "Store", date,
                new BigDecimal("100.00"), months, null, owner);
    }
}
