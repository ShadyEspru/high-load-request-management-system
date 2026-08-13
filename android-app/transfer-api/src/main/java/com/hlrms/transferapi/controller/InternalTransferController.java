package com.hlrms.transferapi.controller;

import com.hlrms.transferapi.dto.ExecuteTransferRequest;
import com.hlrms.transferapi.dto.ExecuteTransferResponse;
import com.hlrms.transferapi.service.MoneyTransferService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping(
    "/internal/v1/transfers"
)
public class InternalTransferController {

    private static final String
        INTERNAL_KEY_HEADER =
            "X-Internal-Service-Key";

    private final MoneyTransferService
        moneyTransferService;

    private final String
        expectedInternalSecret;

    public InternalTransferController(
            MoneyTransferService moneyTransferService,

            @Value(
                "${TRANSFER_INTERNAL_SECRET:}"
            )
            String expectedInternalSecret
    ) {

        this.moneyTransferService =
            moneyTransferService;

        this.expectedInternalSecret =
            expectedInternalSecret;
    }

    @PostMapping("/execute")
    public ExecuteTransferResponse execute(

            @RequestHeader(
                value = INTERNAL_KEY_HEADER,
                required = false
            )
            String providedInternalSecret,

            @Valid
            @RequestBody
            ExecuteTransferRequest request
    ) {

        validateInternalSecret(
            providedInternalSecret
        );

        return moneyTransferService
            .execute(
                request
            );
    }

    private void validateInternalSecret(
            String providedSecret
    ) {

        if (
            expectedInternalSecret == null ||
            expectedInternalSecret.isBlank()
        ) {

            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Internal service authentication " +
                "is not configured"
            );
        }

        if (
            providedSecret == null ||
            providedSecret.isBlank()
        ) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Forbidden"
            );
        }

        boolean matches =
            MessageDigest.isEqual(

                expectedInternalSecret
                    .getBytes(
                        StandardCharsets.UTF_8
                    ),

                providedSecret
                    .getBytes(
                        StandardCharsets.UTF_8
                    )
            );

        if (!matches) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Forbidden"
            );
        }
    }
}
