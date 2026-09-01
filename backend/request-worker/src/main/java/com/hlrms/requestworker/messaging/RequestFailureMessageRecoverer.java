package com.hlrms.requestworker.messaging;

import com.hlrms.requestworker.event.RequestCreatedEvent;
import com.hlrms.requestworker.service.RequestStatusTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestFailureMessageRecoverer
    implements MessageRecoverer {

    private static final int
        MAX_ERROR_MESSAGE_LENGTH = 2_000;

    private final JacksonJsonMessageConverter
        messageConverter;

    private final RequestStatusTransactionService
        statusTransactionService;

    @Override
    public void recover(
        Message message,
        Throwable cause
    ) {
        try {
            Object convertedMessage =
                messageConverter.fromMessage(message);

            if (
                convertedMessage
                    instanceof RequestCreatedEvent event
            ) {
                String errorMessage =
                    buildErrorMessage(cause);

                statusTransactionService.markAsFailed(
                    event.requestId(),
                    errorMessage
                );

                log.error(
                    "Retries exhausted. Request marked " +
                    "as FAILED. eventId={}, " +
                    "requestId={}, error={}",
                    event.eventId(),
                    event.requestId(),
                    errorMessage,
                    cause
                );
            } else {
                log.error(
                    "Retries exhausted, but recovered " +
                    "message has an unexpected type: {}",
                    convertedMessage == null
                        ? "null"
                        : convertedMessage
                            .getClass()
                            .getName(),
                    cause
                );
            }
        } catch (Exception recoveryException) {
            log.error(
                "Could not mark request as FAILED " +
                "after retry exhaustion",
                recoveryException
            );
        }

        throw new AmqpRejectAndDontRequeueException(
            "Retries exhausted; reject message so " +
            "RabbitMQ routes it to the " +
            "dead-letter queue",
            cause
        );
    }

    private String buildErrorMessage(
        Throwable cause
    ) {
        Throwable rootCause = cause;

        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String message =
            rootCause.getClass().getSimpleName() +
            ": " +
            (
                rootCause.getMessage() == null
                    ? "No error message"
                    : rootCause.getMessage()
            );

        if (
            message.length() <=
            MAX_ERROR_MESSAGE_LENGTH
        ) {
            return message;
        }

        return message.substring(
            0,
            MAX_ERROR_MESSAGE_LENGTH
        );
    }
}