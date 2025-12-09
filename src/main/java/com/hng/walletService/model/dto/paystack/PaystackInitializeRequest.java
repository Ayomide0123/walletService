package com.hng.walletService.model.dto.paystack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaystackInitializeRequest {
    private String email;
    private String amount; // in kobo
    private String reference;

    @JsonProperty("callback_url")
    private String callbackUrl;
}
