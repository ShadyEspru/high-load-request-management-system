package com.hlrms.transferapi.controller;

import com.hlrms.transferapi.dto.CreateTransferProfileRequest;
import com.hlrms.transferapi.dto.RecipientResponse;
import com.hlrms.transferapi.dto.TransferProfileResponse;
import com.hlrms.transferapi.security.CurrentUser;
import com.hlrms.transferapi.security.CurrentUserProvider;
import com.hlrms.transferapi.service.TransferProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfer")
public class TransferProfileController {

    private final TransferProfileService service;
    private final CurrentUserProvider currentUserProvider;

    public TransferProfileController(
            TransferProfileService service,
            CurrentUserProvider currentUserProvider
    ) {
        this.service = service;
        this.currentUserProvider =
            currentUserProvider;
    }

    @PostMapping("/profile")
    public TransferProfileResponse createProfile(
            @RequestHeader("Authorization")
            String authorization,

            @Valid
            @RequestBody
            CreateTransferProfileRequest request
    ) {

        CurrentUser user =
            currentUserProvider
                .fromAuthorizationHeader(
                    authorization
                );

        return service.createOrUpdate(
            user,
            request.displayName()
        );
    }

    @GetMapping("/me")
    public TransferProfileResponse getMine(
            @RequestHeader("Authorization")
            String authorization
    ) {

        CurrentUser user =
            currentUserProvider
                .fromAuthorizationHeader(
                    authorization
                );

        return service.getMine(user);
    }

    @GetMapping(
        "/recipients/{transferId}"
    )
    public RecipientResponse getRecipient(
            @PathVariable
            String transferId
    ) {

        return service.findRecipient(
            transferId
        );
    }
}