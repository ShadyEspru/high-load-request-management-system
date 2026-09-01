package com.hlrms.apigateway.filter;

import com.hlrms.apigateway.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/",
            "/api/v1/perf/",
            "/actuator",
            "/fallback/"
    );

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {

        String path = exchange
                .getRequest()
                .getURI()
                .getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authorizationHeader =
                exchange
                        .getRequest()
                        .getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {

            log.warn(
                    "Unauthorized request. Missing Bearer token. path={}",
                    path
            );

            return unauthorized(
                    exchange,
                    "Missing or invalid Authorization header."
            );
        }

        String token =
                authorizationHeader.substring(BEARER_PREFIX.length());

        Claims claims;

        try {
            claims = jwtService.validateAndExtractClaims(token);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {

            log.warn(
                    "Unauthorized request. Invalid JWT. path={}",
                    path
            );

            return unauthorized(
                    exchange,
                    "Invalid or expired access token."
            );
        }

        String userId =
                jwtService.extractUserId(claims);

        String email =
                jwtService.extractEmail(claims);

        List<String> rolesList =
                jwtService.extractRoles(claims);

        String roles =
                String.join(",", rolesList);

        ServerHttpRequest mutatedRequest =
                exchange
                        .getRequest()
                        .mutate()

                        .headers(headers -> {
                            headers.remove(USER_ID_HEADER);
                            headers.remove(USER_EMAIL_HEADER);
                            headers.remove(USER_ROLES_HEADER);
                        })

                        .header(USER_ID_HEADER, userId)
                        .header(USER_EMAIL_HEADER, email)
                        .header(USER_ROLES_HEADER, roles)

                        .build();

        ServerWebExchange mutatedExchange =
                exchange
                        .mutate()
                        .request(mutatedRequest)
                        .build();

        log.debug(
                "Authenticated request. userId={}, email={}, roles={}, path={}",
                userId,
                email,
                roles,
                path
        );

        return chain.filter(mutatedExchange);
    }
        private Mono<Void> unauthorized(
            ServerWebExchange exchange,
            String message
    ) {

        exchange
                .getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        exchange
                .getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "status": 401,
                  "error": "Unauthorized",
                  "message": "%s"
                }
                """.formatted(message);

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        return exchange
                .getResponse()
                .writeWith(
                        Mono.just(
                                exchange
                                        .getResponse()
                                        .bufferFactory()
                                        .wrap(bytes)
                        )
                );
    }

    private boolean isPublicPath(String path) {

        return PUBLIC_PATHS
                .stream()
                .anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {

        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}