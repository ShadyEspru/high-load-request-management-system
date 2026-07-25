package com.hlrms.requestservice.repository;

import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<RequestEntity, UUID> {

    List<RequestEntity> findAllByOrderByCreatedAtDesc();

    List<RequestEntity> findAllByStatusOrderByCreatedAtDesc(
        RequestStatus status
    );
}