package com.hng.walletService.repository;

import com.hng.walletService.model.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByReference(String reference);
    List<TransactionEntity> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    boolean existsByReference(String reference);
    Optional<TransactionEntity> findByPaystackReference(String paystackReference);
}
