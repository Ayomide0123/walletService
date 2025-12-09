package com.hng.walletService.repository;

import com.hng.walletService.model.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, Long> {
    Optional<WalletEntity> findByUserId(Long userId);
    Optional<WalletEntity> findByWalletNumber(String walletNumber);
    boolean existsByWalletNumber(String walletNumber);

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    Optional<WalletEntity> findByIdForUpdate(Long id);

    // Alternative: Use @Query annotation for database-specific locking
    @Query("SELECT w FROM WalletEntity w WHERE w.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WalletEntity> findByIdForUpdate(@Param("id") Long id);

    // Or simply use the standard method
//    Optional<WalletEntity> findById(Long id);
}
