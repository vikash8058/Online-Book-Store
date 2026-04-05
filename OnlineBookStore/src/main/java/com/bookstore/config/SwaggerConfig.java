package com.bookstore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * SwaggerConfig — UC10
 *
 * Configures Swagger UI for API documentation.
 * After adding this, visit:
 *   http://localhost:8080/swagger-ui/index.html
 *   → See all endpoints with request/response details
 *   → Test endpoints directly from browser
 *   → Authorize with JWT token
 *
 * OpenAPI() → creates the API documentation object
 * Info()    → sets title, version, description shown in Swagger UI
 *
 * SecurityScheme → adds "Authorize" button in Swagger UI
 * → allows pasting JWT token directly in Swagger
 * → all requests from Swagger UI will include Bearer token
 */
@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                /*
                 * API Info — shown at top of Swagger UI	
                 */
                .info(new Info()
                        .title("Online Book Store API")
                        .version("1.0.0")
                        .description(
                                "REST API for Online Book Store. " +
                                "UC7: JWT Auth | UC8: OTP Email | UC9: Google OAuth2"
                        )
                )
                /*
                 * Security Scheme — adds JWT authorization to Swagger UI
                 * Type: HTTP
                 * Scheme: bearer
                 * BearerFormat: JWT
                 * → Shows "Authorize" button in Swagger UI
                 * → Paste your JWT token there
                 * → All requests automatically include Authorization: Bearer <token>
                 */
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth")
                )
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}