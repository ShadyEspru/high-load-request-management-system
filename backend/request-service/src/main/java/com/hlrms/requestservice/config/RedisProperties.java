package com.hlrms.requestservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hlrms.redis")
public class RedisProperties {

    private String idempotencyKeyPrefix =
        "hlrms:idempotency:";

    private String lockKeyPrefix =
        "hlrms:lock:idempotency:";

    private String requestCacheKeyPrefix =
        "hlrms:cache:request:";

    private Duration idempotencyTtl =
        Duration.ofHours(24);

    private Duration lockTtl =
        Duration.ofSeconds(10);

    private Duration requestCacheTtl =
        Duration.ofMinutes(30);

    private Duration lockWaitTimeout =
        Duration.ofSeconds(2);

    private Duration lockRetryInterval =
        Duration.ofMillis(50);
}