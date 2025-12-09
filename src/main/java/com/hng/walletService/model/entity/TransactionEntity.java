package com.hng.walletService.model.entity;

import com.hng.walletService.model.enums.TransactionStatus;
import com.hng.walletService.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_reference", columnList = "reference"),
        @Index(name = "idx_transaction_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_transaction_paystack_ref", columnList = "paystack_reference"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_type", columnList = "type"),
        @Index(name = "idx_transaction_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletEntity wallet;

    @Column(name = "reference", nullable = false, unique = true, length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "recipient_wallet_number", length = 13)
    private String recipientWalletNumber;

    @Column(name = "sender_wallet_number", length = 13)
    private String senderWalletNumber;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "paystack_reference", unique = true, length = 100)
    private String paystackReference;

    @Column(name = "authorization_url", length = 500)
    private String authorizationUrl;

    @Column(name = "previous_balance", precision = 19, scale = 2)
    private BigDecimal previousBalance;

    @Column(name = "new_balance", precision = 19, scale = 2)
    private BigDecimal newBalance;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // Store additional JSON data if needed

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isPending() {
        return this.status == TransactionStatus.PENDING;
    }

    public boolean isSuccess() {
        return this.status == TransactionStatus.SUCCESS;
    }

    public boolean isFailed() {
        return this.status == TransactionStatus.FAILED;
    }

    public boolean isDeposit() {
        return this.type == TransactionType.DEPOSIT;
    }

    public boolean isTransfer() {
        return this.type == TransactionType.TRANSFER;
    }

    public void markAsSuccess() {
        this.status = TransactionStatus.SUCCESS;
    }

    public void markAsFailed() {
        this.status = TransactionStatus.FAILED;
    }

    public void updateBalances(BigDecimal previous, BigDecimal newBalance) {
        this.previousBalance = previous;
        this.newBalance = newBalance;
    }
}
