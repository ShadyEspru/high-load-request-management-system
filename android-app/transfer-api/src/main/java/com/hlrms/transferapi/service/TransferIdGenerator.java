package com.hlrms.transferapi.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TransferIdGenerator {

    private static final char[] ALPHABET =
        "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            .toCharArray();

    private static final int LENGTH = 16;

    private final SecureRandom random =
        new SecureRandom();

    public String generate() {

        StringBuilder value =
            new StringBuilder(LENGTH);

        for (int i = 0; i < LENGTH; i++) {
            value.append(
                ALPHABET[
                    random.nextInt(
                        ALPHABET.length
                    )
                ]
            );
        }

        return value.toString();
    }
}