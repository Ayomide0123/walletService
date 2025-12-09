package com.hng.walletService.model.dto.paystack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaystackVerifyResponse {
    private boolean status;
    private String message;
    private PaystackVerifyData data;

    @Data
    public static class PaystackVerifyData {
        private String reference;
        private String status;
        private Long amount; // in kobo

        @JsonProperty("paid_at")
        private String paidAt;

        @JsonProperty("created_at")
        private String createdAt;
    }
}
