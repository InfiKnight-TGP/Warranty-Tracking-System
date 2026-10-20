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
@Table(name = "claim_updates")
public class ClaimUpdate extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "claim_id", nullable = false) private Claim claim;
    @Enumerated(EnumType.STRING) @Column(length = 30) private ClaimStatus oldStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ClaimStatus newStatus;
    @Column(length = 1000) private String note;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "updated_by_id", nullable = false) private User updatedBy;

    protected ClaimUpdate() { }
    public ClaimUpdate(Claim claim, ClaimStatus oldStatus, ClaimStatus newStatus, String note, User updatedBy) {
        this.claim = claim; this.oldStatus = oldStatus; this.newStatus = newStatus; this.note = note; this.updatedBy = updatedBy;
    }
    public Long getId() { return id; }
    public Claim getClaim() { return claim; }
    public ClaimStatus getOldStatus() { return oldStatus; }
    public ClaimStatus getNewStatus() { return newStatus; }
    public String getNote() { return note; }
    public User getUpdatedBy() { return updatedBy; }
}
