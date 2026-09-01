package com.hlrms.requestservice.config;

public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String REQUEST_EXCHANGE =
        "hlrms.request.exchange";

    public static final String REQUEST_QUEUE =
        "hlrms.request.processing.queue";

    public static final String REQUEST_ROUTING_KEY =
        "request.created";


    // Dead Letter Queue

    public static final String REQUEST_DEAD_LETTER_EXCHANGE =
        "hlrms.request.dlx";

    public static final String REQUEST_DEAD_LETTER_QUEUE =
        "hlrms.request.processing.dlq";

    public static final String REQUEST_DEAD_LETTER_ROUTING_KEY =
        "request.failed";
}
