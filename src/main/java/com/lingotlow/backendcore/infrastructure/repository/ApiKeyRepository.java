package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {
}
