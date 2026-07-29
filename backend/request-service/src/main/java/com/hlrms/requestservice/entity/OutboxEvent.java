package com.hlrms.requestservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(
            name = "idx_outbox_events_pending",
            columnList = "status, created_at"
        ),
        @Index(
            name = "idx_outbox_events_aggregate_id",
            columnList = "aggregate_id"
        ),
        @Index(
            name = "idx_outbox_events_event_type",
            columnList = "event_type"
        )
    }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @Column(
        name = "id",
        nullable = false,
        updatable = false
    )
    private UUID id;

    @Column(
        name = "aggregate_id",
        nullable = false,
        updatable = false
    )
    private UUID aggregateId;

    @Column(
        name = "aggregate_type",
        nullable = false,
        length = 100,
        updatable = false
    )
    private String aggregateType;

    @Column(
        name = "event_type",
        nullable = false,
        length = 100,
        updatable = false
    )
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "payload",
        nullable = false,
        columnDefinition = "jsonb",
        updatable = false
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 30
    )
    private OutboxEventStatus status;

    @Column(
        name = "retry_count",
        nullable = false
    )
    private int retryCount;

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(
        name = "last_error",
        columnDefinition = "TEXT"
    )
    private String lastError;

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = OutboxEventStatus.PENDING;
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markAsPublished() {
        status = OutboxEventStatus.PUBLISHED;
        publishedAt = Instant.now();
        lastError = null;
    }

    public void markAsProcessing() {
        status = OutboxEventStatus.PROCESSING;
    }

    public void markAsFailed(String errorMessage) {
        status = OutboxEventStatus.FAILED;
        retryCount++;
        lastError = errorMessage;
    }

    public void markAsPendingForRetry(String errorMessage) {
        status = OutboxEventStatus.PENDING;
        retryCount++;
        lastError = errorMessage;
    }
}