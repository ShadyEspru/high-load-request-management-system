package com.hlrms.transferapi.controller;

import com.hlrms.transferapi.dto.TransferHistoryResponse;
import com.hlrms.transferapi.security.CurrentUser;
import com.hlrms.transferapi.security.CurrentUserProvider;
import com.hlrms.transferapi.service.TransferHistoryService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
    "/api/v1/transfer"
)
public class TransferHistoryController {

    private final TransferHistoryService
        historyService;

    private final CurrentUserProvider
        currentUserProvider;

    public TransferHistoryController(
            TransferHistoryService historyService,
            CurrentUserProvider currentUserProvider
    ) {
        this.historyService =
            historyService;

        this.currentUserProvider =
            currentUserProvider;
    }

    @GetMapping("/history")
    public List<TransferHistoryResponse> history(
            @RequestHeader("Authorization")
            String authorization
    ) {

        CurrentUser user =
            currentUserProvider
                .fromAuthorizationHeader(
                    authorization
                );

        return historyService
            .getHistory(
                user
            );
    }
}
