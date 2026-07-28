package com.hlrms.requestworker.messaging;

import com.hlrms.requestworker.config.RabbitMqConstants;
import com.hlrms.requestworker.event.RequestCreatedEvent;
import com.hlrms.requestworker.service.RequestProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestEventConsumer {

    private final RequestProcessingService requestProcessingService;

    @RabbitListener(
        queues = RabbitMqConstants.REQUEST_QUEUE
    )
    public void consumeRequestCreated(
        RequestCreatedEvent event
    ) {
        log.info(
            "Received RequestCreatedEvent. " +
            "eventId={}, requestId={}, eventType={}, " +
            "eventVersion={}, occurredAt={}",
            event.eventId(),
            event.requestId(),
            event.eventType(),
            event.eventVersion(),
            event.occurredAt()
        );

        if (!"REQUEST_CREATED".equals(event.eventType())) {
            throw new IllegalArgumentException(
                "Unsupported event type: " +
                event.eventType()
            );
        }

        requestProcessingService.processRequest(
            event.requestId()
        );
    }
}
