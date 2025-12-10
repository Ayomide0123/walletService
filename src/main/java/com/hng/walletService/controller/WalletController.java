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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Slf4j
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Tag(
        name = "Wallet Management",
        description = "Endpoint for managing Wallet Operations: deposits, transfers, balance and transaction history."
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
            Creates a Paystack payment link that the user can use to complete the deposit.
            Requires JWT authentication or an API key with **deposit** permission.
            """
    )
    public ResponseEntity<DepositResponse> deposit(
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
//                return ApiResponse.error("API key does not have deposit permission");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String email = authenticationUtil.extractEmail(authentication);
            UserEntity user = userDetailsService.getUserByEmail(email);

            DepositResponse response = transactionService.initiateDeposit(user, request);
//            return ApiResponse.success("Deposit initiated successfully", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error initiating deposit: {}", e.getMessage());
//            return ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/paystack/webhook")
    @Operation(
            summary = "Paystack webhook callback",
            description = """
            Paystack webhook endpoint used by Paystack to notify your service about payment events.
            This endpoint is called by Paystack servers when a payment status changes.
            The webhook signature is verified to ensure the request is authentic.
            """
    )
    public ResponseEntity<Map<String, Object>> paystackWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            HttpServletRequest request) {
        try {
            String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            if (signature == null || !paystackService.verifyWebhookSignature(rawBody, signature)) {
                log.error("Invalid webhook signature");
//                return ApiResponse.error("Invalid signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", false));
            }

            PaystackWebhookPayload webhookPayload = objectMapper.readValue(rawBody, PaystackWebhookPayload.class);

            if ("charge.success".equals(webhookPayload.getEvent())) {
                String reference = webhookPayload.getData().getReference();
                Long amountInKobo = webhookPayload.getData().getAmount();
                BigDecimal amount = new BigDecimal(amountInKobo).divide(new BigDecimal("100"));

                transactionService.processSuccessfulDeposit(reference, amount);
                log.info("Webhook processed successfully for reference: {}", reference);
            }

//            return ApiResponse.success("Webhook processed", "success");
            return ResponseEntity.ok(Map.of("status", true));
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
//            return ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", false));
        }
    }

    @GetMapping("/deposit/{reference}/status")
    @Operation(
            summary = "Get deposit status",
            description = """
            Retrieves the current status of a deposit transaction using its reference ID.
            Returns information about whether the deposit is pending, successful, or failed.
            """
    )
    public ResponseEntity<DepositStatusResponse> getDepositStatus(@PathVariable String reference) {
        try {
            DepositStatusResponse response = transactionService.getDepositStatus(reference);
//            return ApiResponse.success(response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting deposit status: {}", e.getMessage());
//            return ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/balance")
    @Operation(
            summary = "Get wallet balance",
            description = """
            Retrieves the current balance and wallet number for the authenticated user.
            Requires JWT authentication or an API key with **read** permission.
            """
    )
    public ResponseEntity<BalanceResponse> getBalance(Authentication authentication, HttpServletRequest httpRequest) {
        try {
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "read")) {
//                return ApiResponse.error("API key does not have read permission");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String email = authenticationUtil.extractEmail(authentication);
            UserEntity user = userDetailsService.getUserByEmail(email);
            WalletEntity wallet = walletService.getWalletByUser(user);

            BalanceResponse response = BalanceResponse.builder()
                    .balance(wallet.getBalance())
                    .walletNumber(wallet.getWalletNumber())
                    .build();

//            return ApiResponse.success(response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting balance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/transfer")
    @Operation(
            summary = "Transfer funds",
            description = """
            Transfers funds from the authenticated user's wallet to another wallet.
            The recipient is identified by their wallet number.
            Requires JWT authentication or an API key with **transfer** permission.
            """
    )
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "transfer")) {
//                return ApiResponse.error("API key does not have transfer permission");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String email = authenticationUtil.extractEmail(authentication);
            UserEntity user = userDetailsService.getUserByEmail(email);
            TransferResponse response = transactionService.transfer(user, request);
//            return ApiResponse.success("Transfer completed successfully", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing transfer: {}", e.getMessage());
//            return ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/transactions")
    @Operation(
            summary = "Get transaction history",
            description = """
            Retrieves the complete transaction history for the authenticated user.
            Returns all deposits, transfers, and other wallet transactions.
            Requires JWT authentication or an API key with **read** permission.
            """
    )
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "read")) {
//                return ApiResponse.error("API key does not have read permission");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String email = authenticationUtil.extractEmail(authentication);
            UserEntity user = userDetailsService.getUserByEmail(email);
            List<TransactionResponse> transactions = transactionService.getTransactionHistory(user);
//            return ApiResponse.success(transactions);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error getting transactions: {}", e.getMessage());
//            return ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/verify-payment")
    @Operation(
            summary = "Verify Paystack payment after redirect",
            description = """
            Callback endpoint for Paystack redirect after customer completes payment.
            Verifies the transaction with Paystack, updates the deposit status in the database,
            and returns the final transaction status to the client.
            This endpoint is typically called when the user is redirected back from Paystack.
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
