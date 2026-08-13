package com.hlrms.requestworker.service;

public class TransferRejectedException
        extends RuntimeException {

    public TransferRejectedException(
            String message
    ) {
        super(message);
    }
}
