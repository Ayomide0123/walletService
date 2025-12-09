package com.hng.walletService.security;


import com.hng.walletService.model.entity.ApiKeyEntity;
import com.hng.walletService.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String apiKeyHeader = request.getHeader("x-api-key");

        // If no API key header, skip this filter
        if (apiKeyHeader == null || apiKeyHeader.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate API key
            ApiKeyEntity apiKey = apiKeyService.validateApiKey(apiKeyHeader);

            if (apiKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Store API key in request attribute for permission checking
                request.setAttribute("apiKey", apiKey);
                request.setAttribute("userId", apiKey.getUser().getId());

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                apiKey.getUser().getEmail(),
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY"))
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("API key authentication successful for user: {}", apiKey.getUser().getEmail());
            }
        } catch (Exception e) {
            log.error("API Key validation failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
