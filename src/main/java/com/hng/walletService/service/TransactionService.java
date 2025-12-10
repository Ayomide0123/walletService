package com.hng.walletService.service;

import com.hng.walletService.model.dto.paystack.PaystackInitializeResponse;
import com.hng.walletService.model.dto.request.DepositRequest;
import com.hng.walletService.model.dto.request.TransferRequest;
import com.hng.walletService.model.dto.response.DepositResponse;
import com.hng.walletService.model.dto.response.DepositStatusResponse;
import com.hng.walletService.model.dto.response.TransactionResponse;
import com.hng.walletService.model.dto.response.TransferResponse;
import com.hng.walletService.model.entity.TransactionEntity;
import com.hng.walletService.model.entity.UserEntity;
import com.hng.walletService.model.entity.WalletEntity;
import com.hng.walletService.model.enums.TransactionStatus;
import com.hng.walletService.model.enums.TransactionType;
import com.hng.walletService.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final PaystackService paystackService;

    @Transactional
    public DepositResponse initiateDeposit(UserEntity user, DepositRequest request) {
        WalletEntity wallet = walletService.getWalletByUser(user);
        String reference = generateReference();

        // Create pending transaction
        TransactionEntity transaction = TransactionEntity.builder()
                .wallet(wallet)
                .reference(reference)
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .status(TransactionStatus.PENDING)
                .description("Wallet deposit")
                .previousBalance(wallet.getBalance())
                .build();

        transactionRepository.save(transaction);

        // Initialize Paystack transaction
        PaystackInitializeResponse paystackResponse = paystackService.initializeTransaction(
                user.getEmail(),
                request.getAmount(),
                reference
        );

        log.info("paystackResponse {}", paystackResponse);

        // Update transaction with Paystack details
        transaction.setPaystackReference(paystackResponse.getData().getReference());
        transaction.setAuthorizationUrl(paystackResponse.getData().getAuthorizationUrl());
        transactionRepository.save(transaction);

        log.info("Deposit initiated for user: {} with reference: {}", user.getEmail(), reference);

        return DepositResponse.builder()
                .reference(reference)
                .authorizationUrl(paystackResponse.getData().getAuthorizationUrl())
                .build();
    }

    @Transactional
    public void processSuccessfulDeposit(String paystackReference, BigDecimal amount) {
        TransactionEntity transaction = transactionRepository.findByPaystackReference(paystackReference)
                .orElseThrow(() -> new RuntimeException("Transaction not found with paystack reference: " + paystackReference));

        // Prevent double credit - idempotency check
        if (transaction.isSuccess()) {
            log.warn("Transaction already processed: {}", paystackReference);
            return;
        }

        WalletEntity wallet = transaction.getWallet();
        BigDecimal previousBalance = wallet.getBalance();

        // Credit wallet
        walletService.creditWallet(wallet, amount);

        // Update transaction
        transaction.markAsSuccess();
        transaction.updateBalances(previousBalance, wallet.getBalance());
        transactionRepository.save(transaction);

        log.info("Deposit processed successfully: {} for amount: {}", paystackReference, amount);
    }

    @Transactional
    public TransferResponse transfer(UserEntity sender, TransferRequest request) {
        WalletEntity senderWallet = walletService.getWalletByUser(sender);
        WalletEntity recipientWallet = walletService.getWalletByWalletNumber(request.getWalletNumber());

        // Validate sender cannot transfer to self
        if (senderWallet.getWalletNumber().equals(recipientWallet.getWalletNumber())) {
            throw new RuntimeException("Cannot transfer to your own wallet");
        }

        // Check sufficient balance
        if (!senderWallet.hasSufficientBalance(request.getAmount())) {
            throw new RuntimeException("Insufficient balance");
        }

        // Check both wallets are active
        if (!senderWallet.getIsActive() || !recipientWallet.getIsActive()) {
            throw new RuntimeException("Wallet is not active");
        }

        String reference = generateReference();

        // Get balances before transfer
        BigDecimal senderPreviousBalance = senderWallet.getBalance();
        BigDecimal recipientPreviousBalance = recipientWallet.getBalance();

        // Debit sender
        walletService.debitWallet(senderWallet, request.getAmount());

        // Credit recipient
        walletService.creditWallet(recipientWallet, request.getAmount());

        // Create debit transaction for sender
        TransactionEntity debitTransaction = TransactionEntity.builder()
                .wallet(senderWallet)
                .reference(reference + "-DEBIT")
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .recipientWalletNumber(recipientWallet.getWalletNumber())
                .description("Transfer to " + recipientWallet.getWalletNumber())
                .previousBalance(senderPreviousBalance)
                .newBalance(senderWallet.getBalance())
                .build();

        transactionRepository.save(debitTransaction);

        // Create credit transaction for recipient
        TransactionEntity creditTransaction = TransactionEntity.builder()
                .wallet(recipientWallet)
                .reference(reference + "-CREDIT")
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .senderWalletNumber(senderWallet.getWalletNumber())
                .description("Transfer from " + senderWallet.getWalletNumber())
                .previousBalance(recipientPreviousBalance)
                .newBalance(recipientWallet.getBalance())
                .build();

        transactionRepository.save(creditTransaction);

        log.info("Transfer completed: {} from {} to {}",
                request.getAmount(),
                senderWallet.getWalletNumber(),
                recipientWallet.getWalletNumber());

        return TransferResponse.builder()
                .status("success")
                .message("Transfer completed")
                .reference(reference)
                .build();
    }

    public List<TransactionResponse> getTransactionHistory(UserEntity user) {
        WalletEntity wallet = walletService.getWalletByUser(user);
        List<TransactionEntity> transactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DepositStatusResponse getDepositStatus(String reference) {
        TransactionEntity transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        return DepositStatusResponse.builder()
                .reference(reference)
                .status(transaction.getStatus().name().toLowerCase())
                .amount(transaction.getAmount())
                .build();
    }

    private TransactionResponse mapToResponse(TransactionEntity transaction) {
        return TransactionResponse.builder()
                .reference(transaction.getReference())
                .type(transaction.getType().name().toLowerCase())
                .amount(transaction.getAmount())
                .status(transaction.getStatus().name().toLowerCase())
                .description(transaction.getDescription())
                .recipientWalletNumber(transaction.getRecipientWalletNumber())
                .senderWalletNumber(transaction.getSenderWalletNumber())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
