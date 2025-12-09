package com.hng.walletService.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Wallet number is required")
    @Pattern(regexp = "^\\d{13}$", message = "Wallet number must be 13 digits")
    private String walletNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10.0", message = "Minimum transfer amount is 10")
    private BigDecimal amount;
}
