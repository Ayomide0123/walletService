package com.hng.walletService.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Utility class for extracting user information from different authentication types
 */
@Component
public class AuthenticationUtil {

    /**
     * Extracts the user's email from the authentication object.
     * Handles both OAuth2 and JWT-based authentication.
     *
     * @param authentication the Spring Security authentication object
     * @return the user's email address
     * @throws IllegalArgumentException if email cannot be extracted
     */
    public String extractEmail(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            String email = oauth2Token.getPrincipal().getAttribute("email");

            if (email == null) {
                throw new IllegalArgumentException("Email not found in OAuth2 authentication");
            }

            return email;
        }

        // For JWT-based auth, the principal name is the email
        return authentication.getName();
    }

    /**
     * Extracts the user's name from the authentication object.
     *
     * @param authentication the Spring Security authentication object
     * @return the user's name, or null if not available
     */
    public String extractName(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            return oauth2Token.getPrincipal().getAttribute("name");
        }

        return authentication.getName();
    }

    /**
     * Extracts the Google ID from OAuth2 authentication.
     *
     * @param authentication the Spring Security authentication object
     * @return the Google user ID (sub claim), or null if not OAuth2
     */
    public String extractGoogleId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            return oauth2Token.getPrincipal().getAttribute("sub");
        }

        return null;
    }

    /**
     * Checks if the authentication is OAuth2-based.
     *
     * @param authentication the Spring Security authentication object
     * @return true if OAuth2, false otherwise
     */
    public boolean isOAuth2Authentication(Authentication authentication) {
        return authentication instanceof OAuth2AuthenticationToken;
    }
}