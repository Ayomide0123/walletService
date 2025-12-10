package com.hng.walletService.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolloverApiKeyRequest {

    @NotBlank(message = "Expired key ID is required")
    @JsonProperty("expired_key_id")
    private String expiredKeyId;

    @NotBlank(message = "Expiry is required")
    @Pattern(regexp = "^(1H|1D|1M|1Y)$", message = "Expiry must be one of: 1H, 1D, 1M, 1Y")
    private String expiry;
}
