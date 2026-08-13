package com.hlrms.requestworker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class TransferExecutionClient {

    private static final String
        INTERNAL_KEY_HEADER =
            "X-Internal-Service-Key";

    private final RestClient restClient;

    private final String internalSecret;

    private final JsonMapper jsonMapper =
        JsonMapper.builder().build();

    public TransferExecutionClient(

            @Value(
                "${TRANSFER_API_URL:http://localhost:8090}"
            )
            String baseUrl,

            @Value(
                "${TRANSFER_INTERNAL_SECRET:}"
            )
            String internalSecret
    ) {

        this.restClient =
            RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.internalSecret =
            internalSecret;
    }

    public String execute(
            UUID requestId,
            UUID senderUserId,
            String recipientTransferId,
            BigDecimal amount,
            String currency
    ) {

        if (
            internalSecret == null ||
            internalSecret.isBlank()
        ) {

            throw new IllegalStateException(
                "TRANSFER_INTERNAL_SECRET " +
                "is not configured"
            );
        }

        try {

            String response =
                restClient
                    .post()
                    .uri(
                        "/internal/v1/transfers/execute"
                    )
                    .header(
                        INTERNAL_KEY_HEADER,
                        internalSecret
                    )
                    .body(
                        Map.of(
                            "requestId",
                            requestId.toString(),

                            "senderUserId",
                            senderUserId.toString(),

                            "recipientTransferId",
                            recipientTransferId,

                            "amount",
                            amount,

                            "currency",
                            currency
                        )
                    )
                    .retrieve()
                    .body(String.class);

            return response != null
                ? response
                : "Transfer executed";

        } catch (
            RestClientResponseException exception
        ) {

            if (
                exception.getStatusCode()
                    .value() == 409
            ) {

                throw new TransferRejectedException(
                    readBusinessMessage(
                        exception
                            .getResponseBodyAsString()
                    )
                );
            }

            throw exception;
        }
    }

    private String readBusinessMessage(
            String body
    ) {

        try {

            JsonNode root =
                jsonMapper.readTree(body);

            JsonNode message =
                root.get("message");

            if (
                message != null &&
                !message.isNull()
            ) {
                return message.asText();
            }

        } catch (Exception ignored) {
        }

        return "تعذر تنفيذ الحوالة";
    }
}
