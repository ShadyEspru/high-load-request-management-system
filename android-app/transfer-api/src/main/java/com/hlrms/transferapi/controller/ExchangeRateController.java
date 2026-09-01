package com.hlrms.transferapi.controller;

import com.hlrms.transferapi.dto.ExchangeRatesResponse;
import com.hlrms.transferapi.service.ExchangeRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/v1/exchange-rates"
)
public class ExchangeRateController {

    private final ExchangeRateService service;

    public ExchangeRateController(
            ExchangeRateService service
    ) {

        this.service =
                service;
    }

    @GetMapping
    public ExchangeRatesResponse getRates() {

        return service.getRates();
    }
}
