package com.hlrms.apigateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(ResilienceFallbackController.class)
class ResilienceFallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnRequestServiceFallback() {

        webTestClient
            .get()
            .uri("/fallback/request-service")
            .header(
                "X-Correlation-ID",
                "correlation-test-123"
            )
            .exchange()
            .expectStatus()
            .isEqualTo(503)
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(
                HttpHeaders.RETRY_AFTER,
                "10"
            )
            .expectHeader()
            .valueEquals(
                "X-System-Degraded",
                "true"
            )
            .expectHeader()
            .valueEquals(
                "X-Fallback-Service",
                "request-service"
            )
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo(503)
            .jsonPath("$.error")
            .isEqualTo("Service Unavailable")
            .jsonPath("$.service")
            .isEqualTo("request-service")
            .jsonPath("$.degraded")
            .isEqualTo(true)
            .jsonPath("$.retryable")
            .isEqualTo(true)
            .jsonPath("$.retryAfterSeconds")
            .isEqualTo(10)
            .jsonPath("$.message")
            .isEqualTo(
                "Request service is temporarily unavailable."
            )
            .jsonPath("$.path")
            .isEqualTo(
                "/fallback/request-service"
            )
            .jsonPath("$.method")
            .isEqualTo("GET")
            .jsonPath("$.correlationId")
            .isEqualTo(
                "correlation-test-123"
            )
            .jsonPath("$.timestamp")
            .exists();
    }

    @Test
    void shouldReturnAuthServiceFallback() {

        webTestClient
            .post()
            .uri("/fallback/auth-service")
            .exchange()
            .expectStatus()
            .isEqualTo(503)
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(
                HttpHeaders.RETRY_AFTER,
                "10"
            )
            .expectHeader()
            .valueEquals(
                "X-System-Degraded",
                "true"
            )
            .expectHeader()
            .valueEquals(
                "X-Fallback-Service",
                "auth-service"
            )
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo(503)
            .jsonPath("$.error")
            .isEqualTo("Service Unavailable")
            .jsonPath("$.service")
            .isEqualTo("auth-service")
            .jsonPath("$.degraded")
            .isEqualTo(true)
            .jsonPath("$.retryable")
            .isEqualTo(true)
            .jsonPath("$.retryAfterSeconds")
            .isEqualTo(10)
            .jsonPath("$.message")
            .isEqualTo(
                "Authentication service is temporarily unavailable."
            )
            .jsonPath("$.path")
            .isEqualTo(
                "/fallback/auth-service"
            )
            .jsonPath("$.method")
            .isEqualTo("POST")
            .jsonPath("$.timestamp")
            .exists();
    }

    @Test
    void shouldOmitCorrelationIdWhenHeaderIsMissing() {

        webTestClient
            .get()
            .uri("/fallback/request-service")
            .exchange()
            .expectStatus()
            .isEqualTo(503)
            .expectBody()
            .jsonPath("$.correlationId")
            .doesNotExist();
    }
}