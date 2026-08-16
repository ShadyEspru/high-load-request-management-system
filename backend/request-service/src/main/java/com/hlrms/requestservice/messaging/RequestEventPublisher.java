package com.hlrms.requestservice.messaging;

import com.hlrms.requestservice.config.RabbitMqConstants;
import com.hlrms.requestservice.event.RequestCreatedEvent;
import com.hlrms.requestservice.exception.MessagePublishingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestEventPublisher {

    private static final long
        CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;

    public record PendingPublish(
        RequestCreatedEvent event,
        CorrelationData correlationData
    ) {
    }

    public PendingPublish publishRequestCreatedAsync(
        RequestCreatedEvent event
    ) {

        CorrelationData correlationData =
            new CorrelationData(
                event.eventId().toString()
            );

        try {

            rabbitTemplate.convertAndSend(
                RabbitMqConstants.REQUEST_EXCHANGE,
                RabbitMqConstants.REQUEST_ROUTING_KEY,
                event,
                message -> {

                    message.getMessageProperties()
                        .setMessageId(
                            event.eventId().toString()
                        );

                    message.getMessageProperties()
                        .setCorrelationId(
                            event.requestId().toString()
                        );

                    message.getMessageProperties()
                        .setHeader(
                            "eventType",
                            event.eventType()
                        );

                    message.getMessageProperties()
                        .setHeader(
                            "eventVersion",
                            event.eventVersion()
                        );

                    message.getMessageProperties()
                        .setDeliveryMode(
                            MessageDeliveryMode.PERSISTENT
                        );

                    return message;
                },
                correlationData
            );

            return new PendingPublish(
                event,
                correlationData
            );

        } catch (AmqpException exception) {

            throw new MessagePublishingException(
                "Failed to publish request event " +
                "to RabbitMQ",
                exception
            );
        }
    }

    public void awaitConfirmation(
        PendingPublish pendingPublish
    ) {

        RequestCreatedEvent event =
            pendingPublish.event();

        CorrelationData correlationData =
            pendingPublish.correlationData();

        try {

            CorrelationData.Confirm confirm =
                correlationData
                    .getFuture()
                    .get(
                        CONFIRM_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                    );

            if (!confirm.ack()) {

                throw new MessagePublishingException(
                    "RabbitMQ rejected the event. " +
                    "Reason: " +
                    confirm.reason()
                );
            }

            var returned =
                correlationData.getReturned();

            if (returned != null) {

                throw new MessagePublishingException(
                    "RabbitMQ could not route the " +
                    "event. Reply: " +
                    returned.getReplyText()
                );
            }

            log.debug(
                "RabbitMQ confirmed outbox event. " +
                "eventId={}, requestId={}",
                event.eventId(),
                event.requestId()
            );

        } catch (
            MessagePublishingException exception
        ) {

            throw exception;

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new MessagePublishingException(
                "Publishing was interrupted",
                exception
            );

        } catch (TimeoutException exception) {

            throw new MessagePublishingException(
                "Timed out while waiting for " +
                "RabbitMQ confirmation",
                exception
            );

        } catch (ExecutionException exception) {

            throw new MessagePublishingException(
                "Failed while waiting for " +
                "RabbitMQ confirmation",
                exception
            );
        }
    }

    /*
     * Kept for callers/tests that still require
     * synchronous single-message publishing.
     */
    public void publishRequestCreated(
        RequestCreatedEvent event
    ) {

        PendingPublish pendingPublish =
            publishRequestCreatedAsync(
                event
            );

        awaitConfirmation(
            pendingPublish
        );
    }
}
