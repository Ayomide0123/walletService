package com.hng.walletService.service;

import com.hng.walletService.model.entity.UserEntity;
import com.hng.walletService.model.entity.WalletEntity;
import com.hng.walletService.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public WalletEntity createWallet(UserEntity user) {
        // Check if wallet already exists
        if (walletRepository.findByUserId(user.getId()).isPresent()) {
            throw new RuntimeException("Wallet already exists for user");
        }

        String walletNumber = generateUniqueWalletNumber();

        WalletEntity wallet = WalletEntity.builder()
                .user(user)
                .walletNumber(walletNumber)
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .build();

        WalletEntity savedWallet = walletRepository.save(wallet);
        log.info("Wallet created for user: {} with wallet number: {}", user.getEmail(), walletNumber);

        return savedWallet;
    }

    public WalletEntity getWalletByUser(UserEntity user) {
        return walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for user"));
    }

    public WalletEntity getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user"));
    }

    public WalletEntity getWalletByWalletNumber(String walletNumber) {
        return walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    @Transactional
    public void creditWallet(WalletEntity wallet, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (!wallet.getIsActive()) {
            throw new RuntimeException("Wallet is not active");
        }

        wallet.credit(amount);
        walletRepository.save(wallet);
        log.info("Wallet {} credited with {}", wallet.getWalletNumber(), amount);
    }

    @Transactional
    public void debitWallet(WalletEntity wallet, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (!wallet.getIsActive()) {
            throw new RuntimeException("Wallet is not active");
        }

        if (!wallet.hasSufficientBalance(amount)) {
            throw new RuntimeException("Insufficient balance");
        }

        wallet.debit(amount);
        walletRepository.save(wallet);
        log.info("Wallet {} debited with {}", wallet.getWalletNumber(), amount);
    }

    public boolean walletExists(String walletNumber) {
        return walletRepository.existsByWalletNumber(walletNumber);
    }

    private String generateUniqueWalletNumber() {
        String walletNumber;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            walletNumber = generateWalletNumber();
            attempts++;

            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate unique wallet number");
            }
        } while (walletRepository.existsByWalletNumber(walletNumber));

        return walletNumber;
    }

    private String generateWalletNumber() {
        StringBuilder sb = new StringBuilder(13);
        for (int i = 0; i < 13; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
