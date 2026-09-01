package com.hlrms.requestservice.exception;

public class MessagePublishingException
    extends RuntimeException {

    public MessagePublishingException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }

    public MessagePublishingException(String message) {
        super(message);
    }
}