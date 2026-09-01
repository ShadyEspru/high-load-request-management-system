package com.hlrms.requestworker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RequestEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(
        name = "request_type",
        nullable = false,
        length = 100
    )
    private String requestType;

    @Column(
        name = "payload",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 30
    )
    private RequestStatus status;

    @Column(
        name = "result",
        columnDefinition = "TEXT"
    )
    private String result;

    @Column(
        name = "error_message",
        columnDefinition = "TEXT"
    )
    private String errorMessage;

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private Instant createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(
        name = "version",
        nullable = false
    )
    private Long version;

    @Column(
        name = "idempotency_key",
        nullable = false,
        length = 100
    )
    private String idempotencyKey;

    @Column(
        name = "idempotency_fingerprint",
        nullable = false,
        length = 64
    )
    private String idempotencyFingerprint;

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public void markAsProcessing() {
        status = RequestStatus.PROCESSING;
        result = null;
        errorMessage = null;
        completedAt = null;
    }

    public void markAsCompleted(String processingResult) {
        status = RequestStatus.COMPLETED;
        result = processingResult;
        errorMessage = null;
        completedAt = Instant.now();
    }

    public void markAsFailed(String processingError) {
        status = RequestStatus.FAILED;
        result = null;
        errorMessage = processingError;
        completedAt = Instant.now();
    }
}
