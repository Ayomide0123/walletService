package com.hng.walletService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hng.walletService.model.dto.paystack.PaystackVerifyResponse;
import com.hng.walletService.model.dto.paystack.PaystackWebhookPayload;
import com.hng.walletService.model.dto.request.DepositRequest;
import com.hng.walletService.model.dto.request.TransferRequest;
import com.hng.walletService.model.dto.response.*;
import com.hng.walletService.model.entity.ApiKeyEntity;
import com.hng.walletService.model.entity.UserEntity;
import com.hng.walletService.model.entity.WalletEntity;
import com.hng.walletService.service.*;
import com.hng.walletService.util.AuthenticationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Slf4j
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Tag(
        name = "Wallet",
        description = "Operations related to user wallets: deposits, transfers, balance and transaction history."
)
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final PaystackService paystackService;
    private final ApiKeyService apiKeyService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationUtil authenticationUtil;

    @Autowired
    private final ObjectMapper objectMapper;

    @PostMapping("/deposit")
    @Operation(
            summary = "Initiate deposit",
            description = """
            Initiates a deposit for the authenticated user. 
            May require an API key with **deposit** permission.
            """
    )
    public ApiResponse<DepositResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        try {
            // Safely read raw body via ContentCachingRequestWrapper
            if (httpRequest instanceof ContentCachingRequestWrapper wrapper) {
                String rawBody = new String(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
                log.info("Raw body: {}", rawBody);
            } else {
                log.warn("Request not wrapped with ContentCachingRequestWrapper — raw body unavailable.");
            }

            log.info("Authentication {}", authentication);
            log.info("Received deposit request: {}", request);
            log.info("Amount value: {}", request.getAmount());
            log.info("Amount class: {}", request.getAmount() != null ? request.getAmount().getClass() : "null");

            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "deposit")) {
                return ApiResponse.error("API key does not have deposit permission");
            }

            String email = authenticationUtil.extractEmail(authentication);
            UserEntity user = userDetailsService.getUserByEmail(email);

            DepositResponse response = transactionService.initiateDeposit(user, request);
            return ApiResponse.success("Deposit initiated successfully", response);

        } catch (Exception e) {
            log.error("Error initiating deposit: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/paystack/webhook")
    @Operation(
            summary = "Paystack webhook callback",
            description = """
            Paystack webhook endpoint used by Paystack to notify your service about payment events.
            """
    )
    public ApiResponse<String> paystackWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            HttpServletRequest request) {
        try {
            String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            if (signature == null || !paystackService.verifyWebhookSignature(rawBody, signature)) {
                log.error("Invalid webhook signature");
                return ApiResponse.error("Invalid signature");
            }

            PaystackWebhookPayload webhookPayload = objectMapper.readValue(rawBody, PaystackWebhookPayload.class);

            if ("charge.success".equals(webhookPayload.getEvent())) {
                String reference = webhookPayload.getData().getReference();
                Long amountInKobo = webhookPayload.getData().getAmount();
                BigDecimal amount = new BigDecimal(amountInKobo).divide(new BigDecimal("100"));

                transactionService.processSuccessfulDeposit(reference, amount);
                log.info("Webhook processed successfully for reference: {}", reference);
            }

            return ApiResponse.success("Webhook processed", "success");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/deposit/{reference}/status")
    public ApiResponse<DepositStatusResponse> getDepositStatus(@PathVariable String reference) {
        try {
            DepositStatusResponse response = transactionService.getDepositStatus(reference);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Error getting deposit status: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/balance")
    public ApiResponse<BalanceResponse> getBalance(Authentication authentication, HttpServletRequest httpRequest) {
        try {
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "read")) {
                return ApiResponse.error("API key does not have read permission");
            }

            String email = authenticationUtil.extractEmail(authentication);
            UserEntity user = userDetailsService.getUserByEmail(email);
            WalletEntity wallet = walletService.getWalletByUser(user);

            BalanceResponse response = BalanceResponse.builder()
                    .balance(wallet.getBalance())
                    .walletNumber(wallet.getWalletNumber())
                    .build();

            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Error getting balance: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/transfer")
    public ApiResponse<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "transfer")) {
                return ApiResponse.error("API key does not have transfer permission");
            }

            String email = authenticationUtil.extractEmail(authentication);
            UserEntity user = userDetailsService.getUserByEmail(email);
            TransferResponse response = transactionService.transfer(user, request);
            return ApiResponse.success("Transfer completed successfully", response);
        } catch (Exception e) {
            log.error("Error processing transfer: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/transactions")
    public ApiResponse<List<TransactionResponse>> getTransactions(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "read")) {
                return ApiResponse.error("API key does not have read permission");
            }

            String email = authenticationUtil.extractEmail(authentication);
            UserEntity user = userDetailsService.getUserByEmail(email);
            List<TransactionResponse> transactions = transactionService.getTransactionHistory(user);
            return ApiResponse.success(transactions);
        } catch (Exception e) {
            log.error("Error getting transactions: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/verify-payment")
    @Operation(
            summary = "Verify Paystack payment after redirect",
            description = """
        Callback endpoint for Paystack redirect after customer completes payment.
        Verifies the transaction with Paystack, updates the deposit status,
        and returns the final status to the client.
        """
    )
    public ApiResponse<DepositStatusResponse> verifyPayment(
            @RequestParam("reference") String reference,
            @RequestParam(value = "trxref", required = false) String trxref
    ) {
        try {
            log.info("Received verify-payment callback: reference={}, trxref={}", reference, trxref);

            // 1. Call Paystack verify API
            PaystackVerifyResponse verifyResponse = paystackService.verifyTransaction(reference);

            if (verifyResponse == null) {
                log.error("Paystack verify returned null for reference {}", reference);
                return ApiResponse.error("Failed to verify payment with Paystack");
            }

            PaystackVerifyResponse.PaystackVerifyData data = verifyResponse.getData();
            if (data == null) {
                log.error("Paystack verify response has no data for reference {}", reference);
                return ApiResponse.error("Invalid Paystack verify response");
            }

            String paystackStatus = data.getStatus(); // inner "data.status"
            log.info("Paystack verify status for {}: {}", reference, paystackStatus);

            if ("success".equalsIgnoreCase(paystackStatus)) {
                // 2. Convert amount from kobo → Naira
                BigDecimal amount = BigDecimal
                        .valueOf(data.getAmount())  // kobo
                        .divide(new BigDecimal("100"));

                // 3. Update your DB (transaction + wallet)
                // Make sure this method is idempotent (no double credit)
                transactionService.processSuccessfulDeposit(reference, amount);
            } else {
                log.warn("Payment not successful for reference {}. Paystack status={}", reference, paystackStatus);
            }

            // 4. Always return the current status from your DB
            DepositStatusResponse statusResponse = transactionService.getDepositStatus(reference);
            return ApiResponse.success("Payment status fetched successfully", statusResponse);

        } catch (Exception e) {
            log.error("Error verifying payment for reference {}: {}", reference, e.getMessage(), e);
            return ApiResponse.error("An error occurred while verifying payment");
        }
    }




}
