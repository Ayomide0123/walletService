package com.hng.walletService.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("api_key")
    private String apiKey;

    @JsonIgnore
    private String name;

    @JsonIgnore
    private String keyPrefix;

    @JsonIgnore
    private Set<String> permissions;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @JsonIgnore
    private LocalDateTime createdAt;
}

