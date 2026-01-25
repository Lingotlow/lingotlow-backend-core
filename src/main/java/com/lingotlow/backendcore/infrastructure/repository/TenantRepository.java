package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {
  boolean existsByTenantKey(String tenantKey);
  
  Optional<TenantEntity> findByTenantKey(String tenantKey);
  
  Page<TenantEntity> findAll(Pageable pageable);
}
