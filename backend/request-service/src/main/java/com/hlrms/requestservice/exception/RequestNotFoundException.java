package com.hlrms.requestservice.exception;

import java.util.UUID;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(UUID requestId) {
        super("Request not found with id: " + requestId);
    }
}