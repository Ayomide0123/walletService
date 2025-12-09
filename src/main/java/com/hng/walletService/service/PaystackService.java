package com.hng.walletService.service;

import com.hng.walletService.model.dto.paystack.PaystackInitializeRequest;
import com.hng.walletService.model.dto.paystack.PaystackInitializeResponse;
import com.hng.walletService.model.dto.paystack.PaystackVerifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackService {

    @Value("${paystack.secret.key}")
    private String secretKey;

    @Value("${paystack.base.url}")
    private String baseUrl;

    @Value("${paystack.callback.url}")
    private String callbackUrl;

    private final WebClient.Builder webClientBuilder;

    public PaystackInitializeResponse initializeTransaction(String email, BigDecimal amount, String reference) {
        // Convert amount to kobo (multiply by 100)
        String amountInKobo = amount.multiply(new BigDecimal("100")).toPlainString();

        PaystackInitializeRequest request = PaystackInitializeRequest.builder()
                .email(email)
                .amount(amountInKobo)
                .reference(reference)
                .callbackUrl(callbackUrl)
                .build();

        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        try {
            PaystackInitializeResponse response = webClient.post()
                    .uri("/transaction/initialize")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaystackInitializeResponse.class)
                    .block();

            log.info("Paystack transaction initialized: {}", reference);
            return response;
        } catch (Exception e) {
            log.error("Error initializing Paystack transaction: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize payment", e);
        }
    }

    public PaystackVerifyResponse verifyTransaction(String reference) {
        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .build();

        try {
            PaystackVerifyResponse response = webClient.get()
                    .uri("/transaction/verify/" + reference)
                    .retrieve()
                    .bodyToMono(PaystackVerifyResponse.class)
                    .block();

            log.info("Paystack transaction verified: {}", reference);
            return response;
        } catch (Exception e) {
            log.error("Error verifying Paystack transaction: {}", e.getMessage());
            throw new RuntimeException("Failed to verify payment", e);
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(hash);

            return computedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature: {}", e.getMessage());
            return false;
        }
    }
}

