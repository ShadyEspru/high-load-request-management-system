package com.hlrms.transferapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTransferProfileRequest(

        @NotBlank
        @Size(max = 200)
        String displayName

) {
}