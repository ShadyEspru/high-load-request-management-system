package com.hlrms.requestworker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(
        name = "event_id",
        nullable = false,
        updatable = false
    )
    private UUID eventId;

    @Column(
        name = "request_id",
        nullable = false,
        updatable = false
    )
    private UUID requestId;

    @Column(
        name = "event_type",
        nullable = false,
        updatable = false,
        length = 100
    )
    private String eventType;

    @Column(
        name = "event_version",
        nullable = false,
        updatable = false
    )
    private Integer eventVersion;

    @Column(
        name = "occurred_at",
        nullable = false,
        updatable = false
    )
    private Instant occurredAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}