package com.hlrms.transferapi.service;

import com.hlrms.transferapi.dto.RecipientResponse;
import com.hlrms.transferapi.dto.TransferProfileResponse;
import com.hlrms.transferapi.entity.TransferProfileEntity;
import com.hlrms.transferapi.repository.TransferProfileRepository;
import com.hlrms.transferapi.security.CurrentUser;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransferProfileService {

    private final TransferProfileRepository repository;
    private final TransferIdGenerator idGenerator;

    public TransferProfileService(
            TransferProfileRepository repository,
            TransferIdGenerator idGenerator
    ) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    /*
     * تستخدم عند إنشاء الحساب الجديد.
     *
     * إذا كان للمستخدم Profile مسبقًا:
     * - لا نغير Transfer ID.
     * - نحدث displayName فقط.
     *
     * إذا لم يكن موجودًا:
     * - يتم إنشاء Transfer ID تلقائيًا مرة واحدة.
     */
    @Transactional
    public TransferProfileResponse createOrUpdate(
            CurrentUser user,
            String displayName
    ) {

        Instant now =
                Instant.now();

        String normalizedDisplayName =
                normalizeDisplayName(
                        displayName,
                        user
                );

        TransferProfileEntity profile =
                repository
                        .findByUserId(
                                user.id()
                        )
                        .map(existing -> {

                            existing.updateDisplayName(
                                    normalizedDisplayName,
                                    now
                            );

                            return existing;
                        })
                        .orElseGet(() -> {

                            String transferId =
                                    generateUniqueTransferId();

                            return new TransferProfileEntity(
                                    UUID.randomUUID(),
                                    user.id(),
                                    transferId,
                                    user.email(),
                                    normalizedDisplayName,
                                    now,
                                    now
                            );
                        });

        TransferProfileEntity saved =
                repository.save(
                        profile
                );

        return toResponse(
                saved
        );
    }

    /*
     * المستخدم لا يقوم بأي إعداد للاستقبال.
     *
     * Profile موجود:
     *     نعيده بنفس Transfer ID.
     *
     * Profile غير موجود لحساب قديم:
     *     ننشئه تلقائيًا وبصمت.
     */
    @Transactional
    public TransferProfileResponse getMine(
            CurrentUser user
    ) {

        TransferProfileEntity profile =
                repository
                        .findByUserId(
                                user.id()
                        )
                        .orElseGet(() -> {

                            Instant now =
                                    Instant.now();

                            String transferId =
                                    generateUniqueTransferId();

                            String displayName =
                                    defaultDisplayName(
                                            user
                                    );

                            TransferProfileEntity created =
                                    new TransferProfileEntity(
                                            UUID.randomUUID(),
                                            user.id(),
                                            transferId,
                                            user.email(),
                                            displayName,
                                            now,
                                            now
                                    );

                            return repository
                                    .saveAndFlush(
                                            created
                                    );
                        });

        return toResponse(
                profile
        );
    }

    @Transactional(readOnly = true)
    public RecipientResponse findRecipient(
            String transferId
    ) {

        String normalizedTransferId =
                transferId
                        .trim()
                        .toUpperCase();

        TransferProfileEntity profile =
                repository
                        .findByTransferId(
                                normalizedTransferId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Recipient not found"
                                )
                        );

        return new RecipientResponse(
                profile.getTransferId(),
                profile.getDisplayName()
        );
    }

    private String generateUniqueTransferId() {

        for (
                int attempt = 0;
                attempt < 10;
                attempt++
        ) {

            String candidate =
                    idGenerator.generate();

            if (
                    !repository.existsByTransferId(
                            candidate
                    )
            ) {

                return candidate;
            }
        }

        throw new IllegalStateException(
                "Could not generate transfer ID"
        );
    }

    private String normalizeDisplayName(
            String displayName,
            CurrentUser user
    ) {

        if (
                displayName != null &&
                        !displayName.trim().isBlank()
        ) {

            return displayName.trim();
        }

        return defaultDisplayName(
                user
        );
    }

    private String defaultDisplayName(
            CurrentUser user
    ) {

        String email =
                user.email();

        if (
                email != null &&
                        email.contains("@")
        ) {

            String value =
                    email.substring(
                            0,
                            email.indexOf('@')
                    );

            if (!value.isBlank()) {
                return value;
            }
        }

        return "HLRMS User";
    }

    private TransferProfileResponse toResponse(
            TransferProfileEntity profile
    ) {

        String qrContent =
                "hlrms://transfer/" +
                        profile.getTransferId();

        return new TransferProfileResponse(
                profile.getTransferId(),
                profile.getDisplayName(),
                qrContent
        );
    }
}