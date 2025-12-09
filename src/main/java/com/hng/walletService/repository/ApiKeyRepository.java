package com.hng.walletService.repository;

import com.hng.walletService.model.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, Long> {
    Optional<ApiKeyEntity> findByKeyHash(String keyHash);
    List<ApiKeyEntity> findByUserIdAndIsActiveTrue(Long userId);
    long countByUserIdAndIsActiveTrueAndIsRevokedFalse(Long userId);
    Optional<ApiKeyEntity> findByIdAndUserId(Long id, Long userId);
}
