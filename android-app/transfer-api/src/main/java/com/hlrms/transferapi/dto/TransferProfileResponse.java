package com.hlrms.transferapi.dto;

public record TransferProfileResponse(

        String transferId,

        String displayName,

        String qrContent

) {
}