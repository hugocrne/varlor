package com.varlor.backend.product.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration OpenAPI/Swagger pour la documentation de l'API.
 *
 * Définit les métadonnées de l'API, les schémas de sécurité, les serveurs
 * et les tags pour organiser la documentation Swagger UI.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun productOpenAPI(): OpenAPI {
        val securityScheme = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Authentification JWT via Bearer token dans l'en-tête Authorization")

        return OpenAPI()
            .info(
                Info()
                    .title("Varlor Product API")
                    .description(
                        """
                        API REST pour la gestion des utilisateurs, clients et sessions de Varlor.
                        
                        ## Authentification
                        L'API utilise l'authentification JWT (JSON Web Tokens). Pour accéder aux endpoints protégés :
                        1. Obtenez un access token via `/api/auth/login` ou `/api/auth/register`
                        2. Incluez le token dans l'en-tête `Authorization: Bearer <token>`
                        
                        ## Rate Limiting
                        L'API applique un rate limiting pour protéger contre les abus :
                        - Maximum 5 requêtes par minute par IP
                        - Configurable via `app.rate-limiting.*` dans `application.yaml`
                        
                        ## Versioning
                        Version actuelle : 1.0.0
                        """
                    )
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Équipe Varlor")
                            .email("support@varlor.io")
                    )
                    .license(
                        License()
                            .name("Proprietary")
                            .url("https://varlor.io/license")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("Environnement de développement local"),
                    Server()
                        .url("https://api.varlor.io")
                        .description("Environnement de production")
                )
            )
            .tags(
                listOf(
                    Tag()
                        .name("Authentication")
                        .description("Endpoints d'authentification JWT (inscription, connexion, renouvellement de tokens)"),
                    Tag()
                        .name("Utilisateurs")
                        .description("Gestion des utilisateurs (CRUD)"),
                    Tag()
                        .name("Clients")
                        .description("Gestion des clients (CRUD)"),
                    Tag()
                        .name("Sessions utilisateur")
                        .description("Gestion des sessions utilisateur et refresh tokens"),
                    Tag()
                        .name("Préférences utilisateur")
                        .description("Gestion des préférences utilisateur (thème, langue, notifications)"),
                    Tag()
                        .name("Analysis")
                        .description("Opérations d'analyse de données (prétraitement, indicateurs, expressions dynamiques)")
                )
            )
            .components(Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme))
            .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))
    }

    companion object {
        const val SECURITY_SCHEME_NAME = "bearerAuth"
    }
}

