package com.hng.walletService.model.dto.response;

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
    private String reference;
    private String type;
    private BigDecimal amount;
    private String status;
    private String description;
    private String recipientWalletNumber;
    private String senderWalletNumber;
    private LocalDateTime createdAt;
}
