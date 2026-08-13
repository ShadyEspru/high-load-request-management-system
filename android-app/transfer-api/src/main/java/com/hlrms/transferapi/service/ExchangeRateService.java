package com.hlrms.transferapi.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.hlrms.transferapi.dto.ExchangeRatesResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExchangeRateService {

    private static final String PROVIDER_URL =
            "https://open.er-api.com/v6/latest/USD";

    private static final String PROVIDER_NAME =
            "ExchangeRate-API";

    private static final List<String> SUPPORTED_CURRENCIES =
            List.of(
                    "USD",
                    "EUR",
                    "TRY",
                    "SYP"
            );

    private static final MathContext MATH_CONTEXT =
            new MathContext(
                    16,
                    RoundingMode.HALF_UP
            );

    private final JsonMapper objectMapper =
            JsonMapper.builder()
                    .build();

    private final HttpClient httpClient =
            HttpClient
                    .newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(8)
                    )
                    .build();

    private volatile ExchangeRatesResponse cachedResponse;

    private volatile Instant cacheExpiresAt =
            Instant.EPOCH;

    public ExchangeRatesResponse getRates() {

        Instant now =
                Instant.now();

        ExchangeRatesResponse currentCache =
                cachedResponse;

        if (
                currentCache != null &&
                now.isBefore(
                        cacheExpiresAt
                )
        ) {

            return currentCache;
        }

        synchronized (this) {

            now =
                    Instant.now();

            currentCache =
                    cachedResponse;

            if (
                    currentCache != null &&
                    now.isBefore(
                            cacheExpiresAt
                    )
            ) {

                return currentCache;
            }

            try {

                ExchangeRatesResponse freshResponse =
                        fetchFromProvider();

                cachedResponse =
                        freshResponse;

                if (
                        freshResponse.nextUpdateAt() != null &&
                        freshResponse
                                .nextUpdateAt()
                                .isAfter(now)
                ) {

                    cacheExpiresAt =
                            freshResponse
                                    .nextUpdateAt();

                } else {

                    cacheExpiresAt =
                            now.plus(
                                    Duration.ofHours(1)
                            );
                }

                return freshResponse;

            } catch (Exception exception) {

                if (
                        cachedResponse != null
                ) {

                    cacheExpiresAt =
                            now.plus(
                                    Duration.ofMinutes(10)
                            );

                    return cachedResponse;
                }

                throw new IllegalStateException(
                        "Unable to load exchange rates",
                        exception
                );
            }
        }
    }

    private ExchangeRatesResponse fetchFromProvider()
            throws Exception {

        HttpRequest request =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        PROVIDER_URL
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(12)
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString()
                );

        if (
                response.statusCode() < 200 ||
                response.statusCode() >= 300
        ) {

            throw new IllegalStateException(
                    "Exchange rate provider returned HTTP "
                            + response.statusCode()
            );
        }

        JsonNode root =
                objectMapper.readTree(
                        response.body()
                );

        if (
                !"success".equalsIgnoreCase(
                        root.path("result")
                                .asText()
                )
        ) {

            throw new IllegalStateException(
                    "Exchange rate provider returned an error"
            );
        }

        JsonNode providerRates =
                root.path("rates");

        Map<String, BigDecimal> usdRates =
                new LinkedHashMap<>();

        for (
                String currency :
                SUPPORTED_CURRENCIES
        ) {

            JsonNode rateNode =
                    providerRates.path(
                            currency
                    );

            if (
                    rateNode.isMissingNode() ||
                    !rateNode.isNumber()
            ) {

                throw new IllegalStateException(
                        "Missing currency rate: "
                                + currency
                );
            }

            usdRates.put(
                    currency,
                    rateNode.decimalValue()
            );
        }

        Map<String, Map<String, BigDecimal>> crossRates =
                calculateCrossRates(
                        usdRates
                );

        long lastUpdateUnix =
                root.path(
                        "time_last_update_unix"
                )
                        .asLong(0L);

        long nextUpdateUnix =
                root.path(
                        "time_next_update_unix"
                )
                        .asLong(0L);

        Instant updatedAt =
                lastUpdateUnix > 0
                        ? Instant.ofEpochSecond(
                                lastUpdateUnix
                        )
                        : Instant.now();

        Instant nextUpdateAt =
                nextUpdateUnix > 0
                        ? Instant.ofEpochSecond(
                                nextUpdateUnix
                        )
                        : Instant.now()
                                .plus(
                                        Duration.ofHours(1)
                                );

        return new ExchangeRatesResponse(
                updatedAt,
                nextUpdateAt,
                PROVIDER_NAME,
                crossRates
        );
    }

    private Map<String, Map<String, BigDecimal>>
    calculateCrossRates(
            Map<String, BigDecimal> usdRates
    ) {

        Map<String, Map<String, BigDecimal>> result =
                new LinkedHashMap<>();

        for (
                String from :
                SUPPORTED_CURRENCIES
        ) {

            Map<String, BigDecimal> targetRates =
                    new LinkedHashMap<>();

            BigDecimal fromRate =
                    usdRates.get(
                            from
                    );

            for (
                    String to :
                    SUPPORTED_CURRENCIES
            ) {

                if (
                        from.equals(to)
                ) {

                    continue;
                }

                BigDecimal toRate =
                        usdRates.get(
                                to
                        );

                BigDecimal crossRate =
                        toRate.divide(
                                fromRate,
                                MATH_CONTEXT
                        );

                targetRates.put(
                        to,
                        crossRate
                );
            }

            result.put(
                    from,
                    targetRates
            );
        }

        return result;
    }
}
