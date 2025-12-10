package com.hng.walletService.config;

import com.hng.walletService.model.dto.response.JwtResponse;
import com.hng.walletService.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oAuth2AuthenticationToken.getPrincipal();

        // Use your existing logic
        JwtResponse jwtResponse = authService.handleGoogleCallback(oAuth2User);

        log.info("OAuth2 login success for user: {}", jwtResponse.getEmail());

        // Redirect to callback endpoint with the token and some extra info
        String redirectUrl = UriComponentsBuilder
                .fromPath("/auth/google/callback")
                .queryParam("token", jwtResponse.getToken())
                .queryParam("email", jwtResponse.getEmail())
                .queryParam("name", jwtResponse.getName())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
