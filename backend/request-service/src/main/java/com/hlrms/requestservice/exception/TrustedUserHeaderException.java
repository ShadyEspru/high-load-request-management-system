package com.hlrms.requestservice.exception;

public class TrustedUserHeaderException
    extends RuntimeException {

    public TrustedUserHeaderException(
        String message
    ) {
        super(message);
    }

    public TrustedUserHeaderException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}