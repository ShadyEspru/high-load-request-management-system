package com.hlrms.requestservice.repository;


import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RequestRepositoryTest {

    @Autowired
    private RequestRepository requestRepository;



    private RequestEntity createRequest(
        UUID userId,
        String key
    ) {

        return RequestEntity.builder()
            .userId(userId)
            .requestType("TEST")
            .payload("{\"message\":\"hello\"}")
            .idempotencyKey(key)
            .idempotencyFingerprint(
                "fingerprint123"
            )
            .status(
                RequestStatus.PENDING
            )
            .build();
    }



    @Test
    void shouldSaveAndFindRequestByUserId() {


        UUID userId =
            UUID.randomUUID();


        RequestEntity saved =
            requestRepository.save(
                createRequest(
                    userId,
                    "key-1"
                )
            );


        var result =
            requestRepository
                .findByIdAndUserId(
                    saved.getId(),
                    userId
                );


        assertThat(result)
            .isPresent();


        assertThat(
            result.get().getUserId()
        )
        .isEqualTo(userId);
    }



    @Test
    void shouldNotReturnAnotherUsersRequest() {


        UUID userA =
            UUID.randomUUID();

        UUID userB =
            UUID.randomUUID();


        RequestEntity saved =
            requestRepository.save(
                createRequest(
                    userA,
                    "key-a"
                )
            );



        var result =
            requestRepository
                .findByIdAndUserId(
                    saved.getId(),
                    userB
                );



        assertThat(result)
            .isEmpty();
    }




    @Test
    void shouldFindByUserIdAndIdempotencyKey() {


        UUID userId =
            UUID.randomUUID();


        requestRepository.save(
            createRequest(
                userId,
                "same-key"
            )
        );



        var result =
            requestRepository
                .findByUserIdAndIdempotencyKey(
                    userId,
                    "same-key"
                );



        assertThat(result)
            .isPresent();


        assertThat(
            result.get()
                .getIdempotencyKey()
        )
        .isEqualTo(
            "same-key"
        );
    }




    @Test
    void shouldReturnOnlyUserRequests() {


        UUID userA =
            UUID.randomUUID();

        UUID userB =
            UUID.randomUUID();



        requestRepository.save(
            createRequest(
                userA,
                "a-1"
            )
        );


        requestRepository.save(
            createRequest(
                userB,
                "b-1"
            )
        );



        var page =
            requestRepository
                .findAllByUserId(
                    userA,
                    PageRequest.of(
                        0,
                        10,
                        Sort.by(
                            Sort.Direction.DESC,
                            "createdAt"
                        )
                    )
                );



        assertThat(
            page.getContent()
        )
        .hasSize(1);


        assertThat(
            page.getContent()
                .get(0)
                .getUserId()
        )
        .isEqualTo(userA);
    }

}