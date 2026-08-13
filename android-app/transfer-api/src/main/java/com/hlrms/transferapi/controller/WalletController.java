package com.hlrms.transferapi.controller;

import com.hlrms.transferapi.dto.WalletResponse;
import com.hlrms.transferapi.security.CurrentUser;
import com.hlrms.transferapi.security.CurrentUserProvider;
import com.hlrms.transferapi.service.WalletService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/v1/wallet"
)
public class WalletController {

    private final WalletService
        walletService;

    private final CurrentUserProvider
        currentUserProvider;

    public WalletController(
            WalletService walletService,
            CurrentUserProvider currentUserProvider
    ) {

        this.walletService =
            walletService;

        this.currentUserProvider =
            currentUserProvider;
    }

    @GetMapping("/me")
    public WalletResponse getMine(
            @RequestHeader(
                "Authorization"
            )
            String authorization
    ) {

        CurrentUser user =
            currentUserProvider
                .fromAuthorizationHeader(
                    authorization
                );

        return walletService
            .getMine(
                user
            );
    }
}
