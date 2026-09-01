package com.hlrms.apigateway.exception;

import com.hlrms.apigateway.filter.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.net.ConnectException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayExceptionHandler
    implements ErrorWebExceptionHandler {

    private final JsonMapper jsonMapper;

    public GatewayExceptionHandler(
        JsonMapper jsonMapper
    ) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Mono<Void> handle(
        ServerWebExchange exchange,
        Throwable exception
    ) {
        if (
            exchange
                .getResponse()
                .isCommitted()
        ) {
            return Mono.error(exception);
        }

        HttpStatus status =
            resolveStatus(exception);

        String message =
            resolveMessage(status);

        String correlationId =
            exchange.getAttributeOrDefault(
                CorrelationIdFilter
                    .CORRELATION_ID_ATTRIBUTE,
                "unknown"
            );

        Map<String, Object> responseBody =
            new LinkedHashMap<>();

        responseBody.put(
            "timestamp",
            Instant.now().toString()
        );

        responseBody.put(
            "status",
            status.value()
        );

        responseBody.put(
            "error",
            status.getReasonPhrase()
        );

        responseBody.put(
            "message",
            message
        );

        responseBody.put(
            "path",
            exchange
                .getRequest()
                .getURI()
                .getRawPath()
        );

        responseBody.put(
            "correlationId",
            correlationId
        );

        byte[] responseBytes;

        try {
            responseBytes =
                jsonMapper.writeValueAsBytes(
                    responseBody
                );

        } catch (JacksonException serializationException) {
            log.error(
                "Could not serialize gateway error response",
                serializationException
            );

            responseBytes =
                (
                    "{\"status\":500," +
                    "\"error\":\"Internal Server Error\"," +
                    "\"message\":\"Gateway error\"}"
                ).getBytes();
        }

        exchange
            .getResponse()
            .setStatusCode(status);

        exchange
            .getResponse()
            .getHeaders()
            .setContentType(
                MediaType.APPLICATION_JSON
            );

        exchange
            .getResponse()
            .getHeaders()
            .set(
                CorrelationIdFilter
                    .CORRELATION_ID_HEADER,
                correlationId
            );

        DataBuffer dataBuffer =
            exchange
                .getResponse()
                .bufferFactory()
                .wrap(responseBytes);

        log.error(
            "Gateway returned an error response. " +
            "status={}, path={}, correlationId={}, " +
            "exceptionType={}, message={}",
            status.value(),
            exchange
                .getRequest()
                .getURI()
                .getRawPath(),
            correlationId,
            exception
                .getClass()
                .getSimpleName(),
            exception.getMessage()
        );

        return exchange
            .getResponse()
            .writeWith(
                Mono.just(dataBuffer)
            );
    }

    private HttpStatus resolveStatus(
        Throwable exception
    ) {
        Throwable rootCause =
            findRootCause(exception);

        if (
            rootCause instanceof ConnectException
        ) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        if (
            rootCause instanceof TimeoutException
        ) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }

        if (
            exception
                instanceof ResponseStatusException
                    responseStatusException
        ) {
            HttpStatus resolvedStatus =
                HttpStatus.resolve(
                    responseStatusException
                        .getStatusCode()
                        .value()
                );

            if (resolvedStatus != null) {
                return resolvedStatus;
            }
        }

        return HttpStatus.BAD_GATEWAY;
    }

    private String resolveMessage(
        HttpStatus status
    ) {
        return switch (status) {
            case SERVICE_UNAVAILABLE ->
                "The downstream service is unavailable";

            case GATEWAY_TIMEOUT ->
                "The downstream service did not respond in time";

            case NOT_FOUND ->
                "No gateway route was found for this request";

            default ->
                "The gateway could not complete the request";
        };
    }

    private Throwable findRootCause(
        Throwable exception
    ) {
        Throwable current = exception;

        while (
            current.getCause() != null
                && current.getCause() != current
        ) {
            current = current.getCause();
        }

        return current;
    }
}