package com.hng.walletService.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotEmpty(message = "At least one permission is required")
    private Set<String> permissions;

    @NotBlank(message = "Expiry is required")
    @Pattern(regexp = "^(1H|1D|1M|1Y)$", message = "Expiry must be one of: 1H, 1D, 1M, 1Y")
    private String expiry;
}
