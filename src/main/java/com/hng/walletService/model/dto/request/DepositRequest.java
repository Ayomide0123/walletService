package com.hng.walletService.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum deposit amount is 100")
    @DecimalMax(value = "1000000.00", message = "Maximum deposit amount is 1,000,000")
    private BigDecimal amount;
}