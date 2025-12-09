package com.hng.walletService.model.dto.paystack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaystackWebhookPayload {
    private String event;
    private PaystackWebhookData data;

    @Data
    public static class PaystackWebhookData {
        private String reference;
        private String status;
        private Long amount; // in kobo

        @JsonProperty("paid_at")
        private String paidAt;

        @JsonProperty("created_at")
        private String createdAt;

        private String channel;
        private String currency;

        @JsonProperty("ip_address")
        private String ipAddress;

        private PaystackCustomer customer;
    }

    @Data
    public static class PaystackCustomer {
        private String email;

        @JsonProperty("customer_code")
        private String customerCode;
    }
}
