package com.hng.walletService.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Wallet Service API",
                version = "1.0.0",
                description = "Wallet service with Paystack integration, JWT authentication, and API key management",
                contact = @Contact(
                        name = "Wallet Service Team",
                        email = "support@walletservice.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        description = "Production Server",
                        url = "https://masterchief-walletservice.up.railway.app"
                ),
                @Server(
                        description = "Local Development Server",
                        url = "http://localhost:8080"
                )
        }
)
@SecurityScheme(
        name = "Bearer Authentication",
        description = "JWT token authentication. Login with Google to get your JWT token.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
@SecurityScheme(
        name = "API Key Authentication",
        description = "API key authentication for service-to-service communication. Use your API key in the x-api-key header.",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "x-api-key"
)
public class OpenApiConfig {
}