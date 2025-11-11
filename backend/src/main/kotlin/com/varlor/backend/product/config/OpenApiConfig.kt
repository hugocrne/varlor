package com.varlor.backend.product.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun productOpenAPI(): OpenAPI {
        val securityScheme = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")

        return OpenAPI()
            .info(
                Info()
                    .title("Varlor Product API")
                    .description("API de gestion des utilisateurs, clients et sessions de Varlor.")
                    .version("1.0.0")
            )
            .components(Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme))
            .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))
    }

    companion object {
        const val SECURITY_SCHEME_NAME = "bearerAuth"
    }
}

