package com.hng.walletService.controller;

import com.hng.walletService.model.dto.response.ApiResponse;
import com.hng.walletService.model.dto.response.JwtResponse;
import com.hng.walletService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Google OAuth authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/google")
    @Operation(
            summary = "Initiate Google OAuth login",
            description = "Returns the URL to start Google OAuth login"
    )
    public ApiResponse<String> googleLogin() {
        // Frontend can redirect the user to this URL
        return ApiResponse.success("Redirect to Google OAuth", "/oauth2/authorization/google");
    }

    @GetMapping("/google/callback")
    @Operation(
            summary = "Google OAuth callback",
            description = "Receives JWT token from OAuth2 success handler"
    )
    public ApiResponse<JwtResponse> googleCallback(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String error) {

        if (error != null) {
            log.error("Google OAuth error: {}", error);
            return ApiResponse.error("Google authentication failed: " + error);
        }

        if (token == null) {
            log.error("No token supplied in callback");
            return ApiResponse.error("Authentication failed - no token received");
        }

        JwtResponse jwtResponse = JwtResponse.builder()
                .token(token)
                .type("Bearer")
                .email(email)
                .name(name)
                .build();

        log.info("User authenticated successfully via callback: {}", email);

        return ApiResponse.success("Login successful", jwtResponse);
    }
}

