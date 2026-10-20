package com.vault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "claims")
public class Claim extends BaseEntity {
    @Column(nullable = false, length = 2000) private String issueDescription;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "purchase_id", nullable = false) private Purchase purchase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "raised_by_id", nullable = false) private User raisedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_centre_id") private User serviceCentre;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ClaimStatus status = ClaimStatus.RAISED;

    protected Claim() { }
    public Claim(String issueDescription, Purchase purchase, User raisedBy) {
        this.issueDescription = issueDescription; this.purchase = purchase; this.raisedBy = raisedBy;
    }
    public Long getId() { return id; }
    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
    public Purchase getPurchase() { return purchase; }
    public void setPurchase(Purchase purchase) { this.purchase = purchase; }
    public User getRaisedBy() { return raisedBy; }
    public void setRaisedBy(User raisedBy) { this.raisedBy = raisedBy; }
    public User getServiceCentre() { return serviceCentre; }
    public void setServiceCentre(User serviceCentre) { this.serviceCentre = serviceCentre; }
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
}
