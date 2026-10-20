package com.vault.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"productName", "brand", "purchaseDate", "warrantyExpiry", "daysLeft", "status"})
@XmlRootElement(name = "GetWarrantyStatusResponse")
public class GetWarrantyStatusResponse {
    @XmlElement(required = true) private String productName;
    @XmlElement(required = true) private String brand;
    @XmlElement(required = true) private String purchaseDate;
    @XmlElement(required = true) private String warrantyExpiry;
    private long daysLeft;
    @XmlElement(required = true) private String status;

    public GetWarrantyStatusResponse() { }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getWarrantyExpiry() { return warrantyExpiry; }
    public void setWarrantyExpiry(String warrantyExpiry) { this.warrantyExpiry = warrantyExpiry; }
    public long getDaysLeft() { return daysLeft; }
    public void setDaysLeft(long daysLeft) { this.daysLeft = daysLeft; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
