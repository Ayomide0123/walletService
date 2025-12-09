package com.hng.walletService.model.dto.paystack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaystackInitializeResponse {
    private boolean status;
    private String message;
    private PaystackData data;

    @Data
    public static class PaystackData {
        @JsonProperty("authorization_url")
        private String authorizationUrl;

        @JsonProperty("access_code")
        private String accessCode;

        private String reference;
    }
}
