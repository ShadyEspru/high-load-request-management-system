package com.hlrms.requestservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "requests",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_requests_idempotency_key",
            columnNames = "idempotency_key"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Column(
        name = "completed_at"
    )
    private Instant completedAt;

    @Version
    @Column(
        name = "version", nullable = false
    )
    private Long version;

    @Column(
    name = "idempotency_key",
    nullable = false,
    unique = true,
    length = 100
    )
    private String idempotencyKey;

    @Column(
        name = "idempotency_fingerprint",
        nullable = false,
        length = 64
    )
    private String idempotencyFingerprint;

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();

        if (status == null) {
            status = RequestStatus.PENDING;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }
}