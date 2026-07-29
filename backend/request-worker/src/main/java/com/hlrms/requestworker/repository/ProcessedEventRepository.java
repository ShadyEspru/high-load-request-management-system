package com.hlrms.requestworker.repository;

import com.hlrms.requestworker.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedEventRepository
    extends JpaRepository<ProcessedEvent, UUID> {

    @Modifying
    @Query(
        value = """
            INSERT INTO processed_events
            (
                event_id,
                request_id,
                event_type,
                event_version,
                occurred_at,
                processed_at
            )
            VALUES
            (
                :eventId,
                :requestId,
                :eventType,
                :eventVersion,
                :occurredAt,
                NULL
            )
            ON CONFLICT (event_id)
            DO NOTHING
            """,
        nativeQuery = true
    )
    int tryRegisterEvent(
        @Param("eventId")
        UUID eventId,

        @Param("requestId")
        UUID requestId,

        @Param("eventType")
        String eventType,

        @Param("eventVersion")
        int eventVersion,

        @Param("occurredAt")
        Instant occurredAt
    );

    @Modifying
    @Query(
        value = """
            UPDATE processed_events
            SET processed_at = CURRENT_TIMESTAMP
            WHERE event_id = :eventId
              AND processed_at IS NULL
            """,
        nativeQuery = true
    )
    int markEventAsProcessed(
        @Param("eventId")
        UUID eventId
    );
}