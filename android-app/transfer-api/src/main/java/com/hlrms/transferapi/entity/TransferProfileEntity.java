package com.hlrms.transferapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_profiles")
public class TransferProfileEntity {

    @Id
    private UUID id;

    @Column(
        name = "user_id",
        nullable = false,
        unique = true
    )
    private UUID userId;

    @Column(
        name = "transfer_id",
        nullable = false,
        unique = true,
        length = 16
    )
    private String transferId;

    @Column(
        nullable = false,
        length = 320
    )
    private String email;

    @Column(
        name = "display_name",
        nullable = false,
        length = 200
    )
    private String displayName;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private Instant updatedAt;

    protected TransferProfileEntity() {
    }

    public TransferProfileEntity(
            UUID id,
            UUID userId,
            String transferId,
            String email,
            String displayName,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.transferId = transferId;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDisplayName(
            String displayName,
            Instant updatedAt
    ) {
        this.displayName = displayName;
        this.updatedAt = updatedAt;
    }
}