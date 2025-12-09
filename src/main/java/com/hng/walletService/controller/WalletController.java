package com.hng.walletService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hng.walletService.model.dto.paystack.PaystackWebhookPayload;
import com.hng.walletService.model.dto.request.DepositRequest;
import com.hng.walletService.model.dto.request.TransferRequest;
import com.hng.walletService.model.dto.response.*;
import com.hng.walletService.model.entity.ApiKeyEntity;
import com.hng.walletService.model.entity.UserEntity;
import com.hng.walletService.model.entity.WalletEntity;
import com.hng.walletService.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@Slf4j
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Tag(
        name = "Wallet",
        description = "Operations related to user wallets: deposits, transfers, balance and transaction history."
)
@SecurityRequirement(name = "bearerAuth") // Define bearerAuth security scheme in your OpenAPI config
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final PaystackService paystackService;
    private final ApiKeyService apiKeyService;
    private final CustomUserDetailsService userDetailsService;

    @Autowired
    private final ObjectMapper objectMapper;

    @PostMapping("/deposit")
    @Operation(
            summary = "Initiate deposit",
            description = """
            Initiates a deposit for the authenticated user. 
            This endpoint may require an API key with **deposit** permission when called using API key auth.
            The response contains the payment/authorization details needed to complete the deposit.
            """
    )
    public ApiResponse<DepositResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            // Check permissions for API key
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "deposit")) {
                return ApiResponse.error("API key does not have deposit permission");
            }

            UserEntity user = userDetailsService.getUserByEmail(authentication.getName());
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
            This endpoint verifies the Paystack signature and processes successful charge events 
            (e.g., crediting the user's wallet for successful deposits).
            
            **Note:** This is typically called by Paystack servers, not by end-users.
            """
    )
    public ApiResponse<String> paystackWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            HttpServletRequest request) {
        try {
            // Read raw body
            String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            // Verify signature
            if (signature == null || !paystackService.verifyWebhookSignature(rawBody, signature)) {
                log.error("Invalid webhook signature");
                return ApiResponse.error("Invalid signature");
            }

            // Parse payload
//            com.fasterxml.jackson.databind.ObjectMapper objectMapper =
//                    new com.fasterxml.jackson.databind.ObjectMapper();
//            PaystackWebhookPayload webhookPayload = objectMapper.readValue(rawBody, PaystackWebhookPayload.class);
            PaystackWebhookPayload webhookPayload = objectMapper.readValue(rawBody, PaystackWebhookPayload.class);

            // Process only successful charge events
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
    @Operation(
            summary = "Get deposit status",
            description = "Returns the status of a deposit using its unique reference code."
    )
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
    @Operation(
            summary = "Get wallet balance",
            description = """
            Retrieves the current wallet balance and wallet number for the authenticated user.
            When called with an API key, the key must have **read** permission.
            """
    )
    public ApiResponse<BalanceResponse> getBalance(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            // Check permissions for API key
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "read")) {
                return ApiResponse.error("API key does not have read permission");
            }

            UserEntity user = userDetailsService.getUserByEmail(authentication.getName());
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
    @Operation(
            summary = "Transfer funds",
            description = """
            Transfers funds from the authenticated user's wallet to another wallet or destination 
            defined in the transfer request. 
            When called with an API key, the key must have **transfer** permission.
            """
    )
    public ApiResponse<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            // Check permissions for API key
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "transfer")) {
                return ApiResponse.error("API key does not have transfer permission");
            }

            UserEntity user = userDetailsService.getUserByEmail(authentication.getName());
            TransferResponse response = transactionService.transfer(user, request);
            return ApiResponse.success("Transfer completed successfully", response);
        } catch (Exception e) {
            log.error("Error processing transfer: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/transactions")
    @Operation(
            summary = "Get transaction history",
            description = """
            Returns a list of wallet transactions for the authenticated user ordered by most recent.
            When called with an API key, the key must have **read** permission.
            """
    )
    public ApiResponse<List<TransactionResponse>> getTransactions(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            // Check permissions for API key
            ApiKeyEntity apiKey = (ApiKeyEntity) httpRequest.getAttribute("apiKey");
            if (apiKey != null && !apiKeyService.hasPermission(apiKey, "read")) {
                return ApiResponse.error("API key does not have read permission");
            }

            UserEntity user = userDetailsService.getUserByEmail(authentication.getName());
            List<TransactionResponse> transactions = transactionService.getTransactionHistory(user);
            return ApiResponse.success(transactions);
        } catch (Exception e) {
            log.error("Error getting transactions: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
}
