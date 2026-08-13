package com.hlrms.transferapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ExchangeRatesResponse(

        Instant updatedAt,

        Instant nextUpdateAt,

        String provider,

        Map<String, Map<String, BigDecimal>> rates

) {
}
