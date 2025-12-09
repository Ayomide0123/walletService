package com.hng.walletService.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {
    private String apiKey;
    private String name;
    private String keyPrefix;
    private Set<String> permissions;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

