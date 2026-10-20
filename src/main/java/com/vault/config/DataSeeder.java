package com.vault.config;

import com.vault.entity.Purchase;
import com.vault.entity.User;
import com.vault.entity.UserRole;
import com.vault.repository.PurchaseRepository;
import com.vault.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataSeeder(UserRepository userRepository, PurchaseRepository purchaseRepository) {
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            updateDemoPurchasePrices();
            return;
        }

        userRepository.save(new User("Vault Admin", "admin@vault.com", passwordEncoder.encode("admin123"), "9000000001", UserRole.ADMIN));
        userRepository.save(new User("Demo Service Centre", "service@vault.com", passwordEncoder.encode("service123"), "9000000002", UserRole.SERVICE_CENTER));
        User demoUser = userRepository.save(new User("Demo User", "user@vault.com", passwordEncoder.encode("user123"), "9000000003", UserRole.USER));

        LocalDate today = LocalDate.now();
        purchaseRepository.save(purchase("Refrigerator", "Whirlpool", "Appliances", "WR-1001", "Home Store", today.minusMonths(8), 24, "34990.00", demoUser));
        purchaseRepository.save(purchase("Laptop", "Lenovo", "Electronics", "LN-2002", "Tech Market", today.minusMonths(10), 24, "64990.00", demoUser));
        purchaseRepository.save(purchase("Washing Machine", "LG", "Appliances", "LG-3003", "Home Store", today.plusDays(20).minusMonths(12), 12, "28990.00", demoUser));
        purchaseRepository.save(purchase("Smartphone", "Samsung", "Electronics", "SM-4004", "Mobile Hub", today.plusDays(5).minusMonths(12), 12, "24999.00", demoUser));
        purchaseRepository.save(purchase("Television", "Sony", "Electronics", "SY-5005", "Vision Store", today.minusDays(30).minusMonths(12), 12, "54990.00", demoUser));
    }

    private Purchase purchase(String name, String brand, String category, String serial, String store,
                              LocalDate purchaseDate, int warrantyMonths, String price, User owner) {
        return new Purchase(name, brand, category, serial, store, purchaseDate,
                new BigDecimal(price), warrantyMonths, "Support desk", owner);
    }

    private void updateDemoPurchasePrices() {
        userRepository.findByEmail("user@vault.com").ifPresent(demoUser -> {
            Map<String, BigDecimal> demoPrices = Map.of(
                    "WR-1001", new BigDecimal("34990.00"),
                    "LN-2002", new BigDecimal("64990.00"),
                    "LG-3003", new BigDecimal("28990.00"),
                    "SM-4004", new BigDecimal("24999.00"),
                    "SY-5005", new BigDecimal("54990.00"));
            purchaseRepository.findByOwnerId(demoUser.getId()).stream()
                    .filter(purchase -> demoPrices.containsKey(purchase.getSerialNumber()))
                    .forEach(purchase -> purchase.setPrice(demoPrices.get(purchase.getSerialNumber())));
        });
    }
}
