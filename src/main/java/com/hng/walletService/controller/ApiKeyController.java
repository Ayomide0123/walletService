package com.hng.walletService.controller;

import com.hng.walletService.model.dto.request.CreateApiKeyRequest;
import com.hng.walletService.model.dto.request.RolloverApiKeyRequest;
import com.hng.walletService.model.dto.response.ApiKeyResponse;
import com.hng.walletService.model.dto.response.ApiResponse;
import com.hng.walletService.model.entity.UserEntity;
import com.hng.walletService.service.ApiKeyService;
import com.hng.walletService.service.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/keys")
@RequiredArgsConstructor
@Tag(name = "API Key Management", description = "Endpoints for managing API keys")
@SecurityRequirement(name = "Bearer Authentication")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/create")
    @Operation(
            summary = "Create new API key",
            description = "Create a new API key with specific permissions. Maximum 5 active keys per user."
    )
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request,
            Authentication authentication) {
        try {
            UserEntity user = userDetailsService.getUserByEmail(authentication.getName());
            ApiKeyResponse response = apiKeyService.createApiKey(user, request);

            log.info("API key created successfully for user: {}", user.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("API key created successfully", response));
        } catch (RuntimeException e) {
            log.error("Error creating API key: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating API key: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PostMapping("/rollover")
    @Operation(
            summary = "Rollover expired API key",
            description = "Create a new API key using the same permissions as an expired key"
    )
    public ResponseEntity<ApiResponse<ApiKeyResponse>> rolloverApiKey(
            @Valid @RequestBody RolloverApiKeyRequest request,
            Authentication authentication) {
        try {
            UserEntity user = userDetailsService.getUserByEmail(authentication.getName());
            ApiKeyResponse response = apiKeyService.rolloverApiKey(user, request);

            log.info("API key rolled over successfully for user: {}", user.getEmail());

            return ResponseEntity.ok(ApiResponse.success("API key rolled over successfully", response));
        } catch (RuntimeException e) {
            log.error("Error rolling over API key: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error rolling over API key: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @GetMapping("/list")
    @Operation(
            summary = "List all API keys",
            description = "Get all active API keys for the current user"
    )
    public ResponseEntity<ApiResponse<?>> listApiKeys(Authentication authentication) {
        try {
            UserEntity user = userDetailsService.getUserByEmail(authentication.getName());
            var apiKeys = apiKeyService.getUserApiKeys(user);

            return ResponseEntity.ok(ApiResponse.success(apiKeys));
        } catch (Exception e) {
            log.error("Error listing API keys: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve API keys"));
        }
    }

    @DeleteMapping("/{keyId}/revoke")
    @Operation(
            summary = "Revoke API key",
            description = "Revoke an API key to prevent further use"
    )
    public ResponseEntity<ApiResponse<String>> revokeApiKey(
            @PathVariable Long keyId,
            Authentication authentication) {
        try {
            UserEntity user = userDetailsService.getUserByEmail(authentication.getName());
            apiKeyService.revokeApiKey(keyId, user);

            log.info("API key revoked successfully for user: {}", user.getEmail());

            return ResponseEntity.ok(ApiResponse.success("API key revoked successfully", "Key revoked"));
        } catch (RuntimeException e) {
            log.error("Error revoking API key: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error revoking API key: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }
}