package com.hlrms.requestworker.service;

import com.hlrms.requestworker.event.RequestCreatedEvent;
import com.hlrms.requestworker.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentRequestProcessingService {

    private final ProcessedEventRepository
        processedEventRepository;

    private final RequestProcessingService
        requestProcessingService;

    @Transactional
    public void processEvent(
        RequestCreatedEvent event
    ) {
        int insertedRows =
            processedEventRepository.tryRegisterEvent(
                event.eventId(),
                event.requestId(),
                event.eventType(),
                event.eventVersion(),
                event.occurredAt()
            );

        if (insertedRows == 0) {
            log.info(
                "Duplicate event ignored. " +
                "eventId={}, requestId={}",
                event.eventId(),
                event.requestId()
            );

            return;
        }

        log.info(
            "New event registered for processing. " +
            "eventId={}, requestId={}",
            event.eventId(),
            event.requestId()
        );

        requestProcessingService.processRequest(
            event.requestId()
        );

        int updatedRows =
            processedEventRepository
                .markEventAsProcessed(
                    event.eventId()
                );

        if (updatedRows != 1) {
            throw new IllegalStateException(
                "Could not mark event as processed. " +
                "eventId=" +
                event.eventId()
            );
        }

        log.info(
            "Event processing completed and recorded. " +
            "eventId={}, requestId={}",
            event.eventId(),
            event.requestId()
        );
    }
}