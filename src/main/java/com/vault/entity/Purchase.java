package com.vault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "purchases")
@jakarta.persistence.NamedQuery(name = "Purchase.findExpiringBetween",
        query = "SELECT p FROM Purchase p WHERE p.warrantyExpiry BETWEEN :startDate AND :endDate ORDER BY p.warrantyExpiry ASC")
public class Purchase extends BaseEntity {
    @Column(nullable = false, length = 160) private String productName;
    @Column(length = 120) private String brand;
    @Column(length = 100) private String category;
    @Column(length = 120) private String serialNumber;
    @Column(length = 160) private String storeName;
    @Column(nullable = false) private LocalDate purchaseDate;
    @Column(precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false) private int warrantyMonths;
    @Column(nullable = false) private LocalDate warrantyExpiry;
    @Column(length = 500) private String billFilePath;
    @Column(length = 200) private String supportContact;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    @Column(nullable = false) private boolean reminder30Sent;
    @Column(nullable = false) private boolean reminder7Sent;

    protected Purchase() { }
    public Purchase(String productName, String brand, String category, String serialNumber, String storeName,
                    LocalDate purchaseDate, BigDecimal price, int warrantyMonths, String supportContact, User owner) {
        this.productName = productName; this.brand = brand; this.category = category; this.serialNumber = serialNumber;
        this.storeName = storeName; this.purchaseDate = purchaseDate; this.price = price;
        this.warrantyMonths = warrantyMonths; this.warrantyExpiry = purchaseDate.plusMonths(warrantyMonths);
        this.supportContact = supportContact; this.owner = owner;
    }
    @Override
    protected void onCreate() {
        if (warrantyExpiry == null && purchaseDate != null) warrantyExpiry = purchaseDate.plusMonths(warrantyMonths);
    }
    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; recalculateExpiry(); }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; recalculateExpiry(); }
    private void recalculateExpiry() { if (purchaseDate != null) warrantyExpiry = purchaseDate.plusMonths(warrantyMonths); }
    public LocalDate getWarrantyExpiry() { return warrantyExpiry; }
    public String getBillFilePath() { return billFilePath; }
    public void setBillFilePath(String billFilePath) { this.billFilePath = billFilePath; }
    public String getSupportContact() { return supportContact; }
    public void setSupportContact(String supportContact) { this.supportContact = supportContact; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public boolean isReminder30Sent() { return reminder30Sent; }
    public void setReminder30Sent(boolean reminder30Sent) { this.reminder30Sent = reminder30Sent; }
    public boolean isReminder7Sent() { return reminder7Sent; }
    public void setReminder7Sent(boolean reminder7Sent) { this.reminder7Sent = reminder7Sent; }
}
