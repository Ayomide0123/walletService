package com.hng.walletService.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String type;
    private BigDecimal amount;
    private String status;

    @JsonIgnore
    private String reference;

    @JsonIgnore
    private String description;

    @JsonIgnore
    private String recipientWalletNumber;

    @JsonIgnore
    private String senderWalletNumber;

    @JsonIgnore
    private LocalDateTime createdAt;
}
