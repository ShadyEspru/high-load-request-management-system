package com.hlrms.apigateway.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void shouldPreserveValidIncomingCorrelationId() {

        String correlationId =
            "request-123.test_value";

        MockServerWebExchange exchange =
            exchangeWithCorrelationId(
                correlationId
            );

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        GatewayFilterChain chain =
            currentExchange -> {
                forwardedExchange.set(
                    currentExchange
                );

                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        ServerWebExchange result =
            forwardedExchange.get();

        assertThat(result)
            .isNotNull();

        assertThat(
            result
                .getRequest()
                .getHeaders()
                .getFirst(
                    CorrelationIdFilter
                        .CORRELATION_ID_HEADER
                )
        )
        .isEqualTo(correlationId);

        String attributeCorrelationId =
            result.getAttribute(
                CorrelationIdFilter
                    .CORRELATION_ID_ATTRIBUTE
            );

        assertThat(attributeCorrelationId)
            .isEqualTo(correlationId);

        assertThat(
            result
                .getResponse()
                .getHeaders()
                .getFirst(
                    CorrelationIdFilter
                        .CORRELATION_ID_HEADER
                )
        )
        .isEqualTo(correlationId);
    }

    @Test
    void shouldTrimIncomingCorrelationId() {

        MockServerWebExchange exchange =
            exchangeWithCorrelationId(
                "  request-123  "
            );

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange -> {
                    forwardedExchange.set(
                        currentExchange
                    );

                    return Mono.empty();
                }
            )
        )
        .verifyComplete();

        assertThat(
            forwardedExchange
                .get()
                .getRequest()
                .getHeaders()
                .getFirst(
                    CorrelationIdFilter
                        .CORRELATION_ID_HEADER
                )
        )
        .isEqualTo("request-123");
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() {

        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/api/v1/requests")
                    .build()
            );

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange -> {
                    forwardedExchange.set(
                        currentExchange
                    );

                    return Mono.empty();
                }
            )
        )
        .verifyComplete();

        String generatedId =
            forwardedExchange
                .get()
                .getRequest()
                .getHeaders()
                .getFirst(
                    CorrelationIdFilter
                        .CORRELATION_ID_HEADER
                );

        assertThat(generatedId)
            .isNotBlank();

        assertThatCodeIsUuid(generatedId);

        String attributeCorrelationId =
            forwardedExchange
                .get()
                .getAttribute(
                    CorrelationIdFilter
                        .CORRELATION_ID_ATTRIBUTE
                );

        assertThat(attributeCorrelationId)
            .isEqualTo(generatedId);

        assertThat(
            forwardedExchange
                .get()
                .getResponse()
                .getHeaders()
                .getFirst(
                    CorrelationIdFilter
                        .CORRELATION_ID_HEADER
                )
        )
        .isEqualTo(generatedId);
    }

    @Test
    void shouldGenerateCorrelationIdForBlankHeader() {

        MockServerWebExchange exchange =
            exchangeWithCorrelationId("   ");

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange -> {
                    forwardedExchange.set(
                        currentExchange
                    );

                    return Mono.empty();
                }
            )
        )
        .verifyComplete();

        String generatedId =
            forwardedExchange
                .get()
                .getRequest()
                .getHeaders()
                .getFirst(
                    CorrelationIdFilter
                        .CORRELATION_ID_HEADER
                );

        assertThatCodeIsUuid(generatedId);
    }

    @Test
    void shouldReplaceCorrelationIdContainingInvalidCharacters() {

        String invalidId =
            "request id/with?invalid=characters";

        MockServerWebExchange exchange =
            exchangeWithCorrelationId(
                invalidId
            );

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange -> {
                    forwardedExchange.set(
                        currentExchange
                    );

                    return Mono.empty();
                }
            )
        )
        .verifyComplete();

        String generatedId =
            forwardedExchange
                .get()
                .getRequest()
                .getHeaders()
                .getFirst(
                    CorrelationIdFilter
                        .CORRELATION_ID_HEADER
                );

        assertThat(generatedId)
            .isNotEqualTo(invalidId);

        assertThatCodeIsUuid(generatedId);
    }

    @Test
    void shouldReplaceCorrelationIdLongerThan100Characters() {

        String longId =
            "x".repeat(101);

        MockServerWebExchange exchange =
            exchangeWithCorrelationId(
                longId
            );

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange -> {
                    forwardedExchange.set(
                        currentExchange
                    );

                    return Mono.empty();
                }
            )
        )
        .verifyComplete();

        String generatedId =
            forwardedExchange
                .get()
                .getRequest()
                .getHeaders()
                .getFirst(
                    CorrelationIdFilter
                        .CORRELATION_ID_HEADER
                );

        assertThat(generatedId)
            .isNotEqualTo(longId);

        assertThatCodeIsUuid(generatedId);
    }

    @Test
    void shouldReplaceExistingRequestHeaderWithNormalizedValue() {

        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/api/v1/requests")
                    .header(
                        CorrelationIdFilter
                            .CORRELATION_ID_HEADER,
                        " first-value ",
                        "second-value"
                    )
                    .build()
            );

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange -> {
                    forwardedExchange.set(
                        currentExchange
                    );

                    return Mono.empty();
                }
            )
        )
        .verifyComplete();

        HttpHeaders headers =
            forwardedExchange
                .get()
                .getRequest()
                .getHeaders();

        assertThat(
            headers.get(
                CorrelationIdFilter
                    .CORRELATION_ID_HEADER
            )
        )
        .containsExactly("first-value");
    }

    @Test
    void shouldMakeCorrelationIdAvailableDuringChainExecution() {

        String correlationId =
            "mdc-test-123";

        MockServerWebExchange exchange =
            exchangeWithCorrelationId(
                correlationId
            );

        AtomicReference<String> mdcValue =
            new AtomicReference<>();

        GatewayFilterChain chain =
            currentExchange -> {
                mdcValue.set(
                    MDC.get("correlationId")
                );

                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(mdcValue.get())
            .isEqualTo(correlationId);
    }

    @Test
    void shouldRemoveCorrelationIdFromMdcAfterCompletion() {

        MDC.put(
            "correlationId",
            "old-value"
        );

        MockServerWebExchange exchange =
            exchangeWithCorrelationId(
                "new-value"
            );

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange ->
                    Mono.empty()
            )
        )
        .verifyComplete();

        assertThat(
            MDC.get("correlationId")
        )
        .isNull();
    }

    @Test
    void shouldRemoveCorrelationIdFromMdcAfterFailure() {

        MockServerWebExchange exchange =
            exchangeWithCorrelationId(
                "failure-test"
            );

        RuntimeException chainFailure =
            new RuntimeException(
                "Downstream failure"
            );

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange ->
                    Mono.error(chainFailure)
            )
        )
        .expectErrorSatisfies(exception ->
            assertThat(exception)
                .isSameAs(chainFailure)
        )
        .verify();

        assertThat(
            MDC.get("correlationId")
        )
        .isNull();
    }

    @Test
    void shouldHaveHighestPrecedence() {

        assertThat(filter.getOrder())
            .isEqualTo(
                Ordered.HIGHEST_PRECEDENCE
            );
    }

    private MockServerWebExchange
    exchangeWithCorrelationId(
        String correlationId
    ) {
        return MockServerWebExchange.from(
            MockServerHttpRequest
                .get("/api/v1/requests")
                .header(
                    CorrelationIdFilter
                        .CORRELATION_ID_HEADER,
                    correlationId
                )
                .build()
        );
    }

    private void assertThatCodeIsUuid(
        String value
    ) {
        assertThat(value)
            .isNotBlank();

        UUID parsedValue =
            UUID.fromString(value);

        assertThat(parsedValue.toString())
            .isEqualTo(value);
    }
}