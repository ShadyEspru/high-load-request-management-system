package com.hlrms.requestservice.repository;

import com.hlrms.requestservice.entity.OutboxEvent;
import com.hlrms.requestservice.entity.OutboxEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
    extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT event
        FROM OutboxEvent event
        WHERE event.status = :status
        ORDER BY event.createdAt ASC
        """)
    List<OutboxEvent> findBatchForUpdate(
        @Param("status")
        OutboxEventStatus status,
        Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE OutboxEvent event
        SET event.status =
            com.hlrms.requestservice.entity
                .OutboxEventStatus.PENDING
        WHERE event.status =
            com.hlrms.requestservice.entity
                .OutboxEventStatus.PROCESSING
        """)
    int resetProcessingEventsToPending();
}