package com.vault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    @Column(nullable = false, updatable = false)
    protected LocalDateTime createdAt;
    @Column(nullable = false)
    protected LocalDateTime updatedAt;

    @PrePersist
    protected void onBaseCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        onCreate();
    }

    @PreUpdate
    protected void onBaseUpdate() {
        updatedAt = LocalDateTime.now();
        onUpdate();
    }

    protected void onCreate() { }
    protected void onUpdate() { }

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
