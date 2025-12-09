package com.hng.walletService.service;

import com.hng.walletService.model.dto.request.CreateApiKeyRequest;
import com.hng.walletService.model.dto.request.RolloverApiKeyRequest;
import com.hng.walletService.model.dto.response.ApiKeyResponse;
import com.hng.walletService.model.entity.ApiKeyEntity;
import com.hng.walletService.model.entity.UserEntity;
import com.hng.walletService.model.enums.Permission;
import com.hng.walletService.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private static final int MAX_ACTIVE_KEYS = 5;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ApiKeyResponse createApiKey(UserEntity user, CreateApiKeyRequest request) {
        // Check active key limit
        long activeKeyCount = apiKeyRepository.countByUserIdAndIsActiveTrueAndIsRevokedFalse(user.getId());
        if (activeKeyCount >= MAX_ACTIVE_KEYS) {
            throw new RuntimeException("Maximum of 5 active API keys allowed");
        }

        // Validate permissions
        Set<String> permissions = validatePermissions(request.getPermissions());

        // Generate API key
        String apiKey = generateApiKey();
        String keyHash = hashApiKey(apiKey);
        String keyPrefix = apiKey.substring(0, Math.min(8, apiKey.length()));

        // Calculate expiry
        LocalDateTime expiresAt = calculateExpiry(request.getExpiry());

        ApiKeyEntity apiKeyEntity = ApiKeyEntity.builder()
                .user(user)
                .name(request.getName())
                .keyHash(keyHash)
                .keyPrefix(keyPrefix)
                .permissions(permissions)
                .expiresAt(expiresAt)
                .isActive(true)
                .isRevoked(false)
                .build();

        apiKeyRepository.save(apiKeyEntity);

        log.info("API key created for user: {} with name: {}", user.getEmail(), request.getName());

        return ApiKeyResponse.builder()
                .apiKey(apiKey)
                .name(request.getName())
                .keyPrefix(keyPrefix)
                .permissions(permissions)
                .expiresAt(expiresAt)
                .createdAt(apiKeyEntity.getCreatedAt())
                .build();
    }

    @Transactional
    public ApiKeyResponse rolloverApiKey(UserEntity user, RolloverApiKeyRequest request) {
        // Find expired key
        Long keyId = Long.parseLong(request.getExpiredKeyId());
        ApiKeyEntity expiredKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        // Verify ownership
        if (!expiredKey.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to API key");
        }

        // Verify key is expired
        if (!expiredKey.isExpired()) {
            throw new RuntimeException("API key is not expired yet");
        }

        // Check active key limit (excluding the expired one)
        long activeKeyCount = apiKeyRepository.countByUserIdAndIsActiveTrueAndIsRevokedFalse(user.getId());
        if (activeKeyCount >= MAX_ACTIVE_KEYS) {
            throw new RuntimeException("Maximum of 5 active API keys allowed");
        }

        // Generate new API key with same permissions
        String newApiKey = generateApiKey();
        String keyHash = hashApiKey(newApiKey);
        String keyPrefix = newApiKey.substring(0, Math.min(8, newApiKey.length()));
        LocalDateTime expiresAt = calculateExpiry(request.getExpiry());

        ApiKeyEntity newApiKeyEntity = ApiKeyEntity.builder()
                .user(user)
                .name(expiredKey.getName() + " (Rolled over)")
                .keyHash(keyHash)
                .keyPrefix(keyPrefix)
                .permissions(expiredKey.getPermissions())
                .expiresAt(expiresAt)
                .isActive(true)
                .isRevoked(false)
                .build();

        apiKeyRepository.save(newApiKeyEntity);

        // Deactivate the old key
        expiredKey.setIsActive(false);
        apiKeyRepository.save(expiredKey);

        log.info("API key rolled over for user: {}", user.getEmail());

        return ApiKeyResponse.builder()
                .apiKey(newApiKey)
                .name(newApiKeyEntity.getName())
                .keyPrefix(keyPrefix)
                .permissions(newApiKeyEntity.getPermissions())
                .expiresAt(expiresAt)
                .createdAt(newApiKeyEntity.getCreatedAt())
                .build();
    }

    public ApiKeyEntity validateApiKey(String apiKey) {
        String keyHash = hashApiKey(apiKey);

        ApiKeyEntity apiKeyEntity = apiKeyRepository.findByKeyHash(keyHash)
                .orElseThrow(() -> new RuntimeException("Invalid API key"));

        if (!apiKeyEntity.isValid()) {
            throw new RuntimeException("API key is expired or revoked");
        }

        // Update last used timestamp
        apiKeyEntity.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKeyEntity);

        return apiKeyEntity;
    }

    public boolean hasPermission(ApiKeyEntity apiKey, String permission) {
        return apiKey.getPermissions().contains(permission);
    }

    public List<ApiKeyEntity> getUserApiKeys(UserEntity user) {
        return apiKeyRepository.findByUserIdAndIsActiveTrue(user.getId());
    }

    @Transactional
    public void revokeApiKey(Long keyId, UserEntity user) {
        ApiKeyEntity apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to API key");
        }

        apiKey.setIsRevoked(true);
        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);

        log.info("API key revoked for user: {}", user.getEmail());
    }

    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return "sk_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    private LocalDateTime calculateExpiry(String expiry) {
        LocalDateTime now = LocalDateTime.now();
        return switch (expiry) {
            case "1H" -> now.plusHours(1);
            case "1D" -> now.plusDays(1);
            case "1M" -> now.plusMonths(1);
            case "1Y" -> now.plusYears(1);
            default -> throw new IllegalArgumentException("Invalid expiry format: " + expiry);
        };
    }

    private Set<String> validatePermissions(Set<String> permissions) {
        return permissions.stream()
                .map(permission -> {
                    try {
                        return Permission.fromValue(permission).getValue();
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid permission: " + permission);
                    }
                })
                .collect(Collectors.toSet());
    }
}
