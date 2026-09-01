package com.hlrms.requestworker.repository;

import com.hlrms.requestworker.entity.RequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RequestRepository
    extends JpaRepository<RequestEntity, UUID> {
}
