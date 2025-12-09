package com.hng.walletService.controller;

import com.hng.walletService.model.dto.response.ApiResponse;
import com.hng.walletService.model.dto.response.JwtResponse;
import com.hng.walletService.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Google OAuth2 login and JWT issuance for the wallet service."
)
public class AuthController {

    private final AuthService authService;

    @GetMapping("/google")
    @Operation(
            summary = "Get Google OAuth2 authorization URL",
            description = """
            Returns the Google OAuth2 authorization URL used to start the login flow.
            Typically, the frontend will redirect the user to this URL.
            """
    )
    public ApiResponse<String> googleLogin() {
        return ApiResponse.success("Redirect to Google OAuth", "/oauth2/authorization/google");
    }

    @GetMapping("/google/callback")
    @Operation(
            summary = "Google OAuth2 callback",
            description = """
            Callback endpoint invoked after a successful Google OAuth2 login.
            Exchanges the authenticated Google user information for a JWT token.
            
            This endpoint is typically called by Spring Security as part of the OAuth2 flow.
            On success, returns a JWT that can be used to authenticate subsequent API requests.
            """
    )
    public ApiResponse<JwtResponse> googleCallback(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return ApiResponse.error("Authentication failed");
        }

        JwtResponse response = authService.handleGoogleCallback(oAuth2User);
        return ApiResponse.success("Login successful", response);
    }
}
