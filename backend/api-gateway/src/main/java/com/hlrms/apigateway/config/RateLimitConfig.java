package com.hlrms.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    private static final String TRUSTED_USER_ID_HEADER = "X-User-Id";

    @Bean
    public KeyResolver authenticatedUserKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(TRUSTED_USER_ID_HEADER);

            if (userId == null || userId.isBlank()) {
                return Mono.empty();
            }

            return Mono.just("user:" + userId);
        };
    }
}