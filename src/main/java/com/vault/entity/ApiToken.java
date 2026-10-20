package com.vault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_tokens", uniqueConstraints = @UniqueConstraint(name = "uk_api_tokens_hash", columnNames = "token_hash"))
public class ApiToken extends BaseEntity {
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    private LocalDateTime lastUsedAt;
    @Column(nullable = false)
    private boolean revoked;

    protected ApiToken() { }

    public ApiToken(String tokenHash, User user) {
        this.tokenHash = tokenHash;
        this.user = user;
    }

    public String getTokenHash() { return tokenHash; }
    public User getUser() { return user; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
