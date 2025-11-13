package com.varlor.backend.product.model.dto

import com.varlor.backend.product.model.entity.UserRole
import com.varlor.backend.product.model.entity.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "Requête d'inscription d'un nouvel utilisateur")
data class RegisterRequestDto(
    @field:NotNull
    @field:Schema(description = "Identifiant du client auquel appartient l'utilisateur", example = "11111111-2222-3333-4444-555555555555")
    val clientId: UUID,

    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    @field:Schema(description = "Adresse email de l'utilisateur (sera normalisée en lowercase)", example = "nouvel.utilisateur@varlor.io")
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 255)
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial (@$!%*?&)"
    )
    @field:Schema(
        description = "Mot de passe en clair soumis par l'utilisateur, haché côté serveur avant stockage",
        example = "Secret123!",
        minLength = 8,
        maxLength = 255
    )
    val password: String,

    @field:NotBlank
    @field:Size(max = 100)
    @field:Schema(description = "Prénom de l'utilisateur", example = "Nouveau", maxLength = 100)
    val firstName: String,

    @field:NotBlank
    @field:Size(max = 100)
    @field:Schema(description = "Nom de famille de l'utilisateur", example = "Utilisateur", maxLength = 100)
    val lastName: String,

    @field:Schema(description = "Rôle de l'utilisateur", example = "MEMBER", defaultValue = "MEMBER")
    val role: UserRole = UserRole.MEMBER,
    
    @field:Schema(description = "Statut de l'utilisateur", example = "ACTIVE", defaultValue = "ACTIVE")
    val status: UserStatus = UserStatus.ACTIVE
)

@Schema(description = "Requête de connexion utilisateur")
data class LoginRequestDto(
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    @field:Schema(description = "Adresse email de l'utilisateur", example = "owner@varlor.io")
    val email: String,

    @field:NotBlank
    @field:Size(max = 255)
    @field:Schema(description = "Mot de passe de l'utilisateur", example = "Secret123!")
    val password: String
)

@Schema(description = "Requête de renouvellement de jeton")
data class RefreshTokenRequestDto(
    @field:NotBlank
    @field:Size(min = 32)
    @field:Schema(
        description = "Refresh token à utiliser pour générer un nouveau couple de jetons",
        example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
        minLength = 32
    )
    val refreshToken: String
)

@Schema(description = "Requête de déconnexion utilisateur")
data class LogoutRequestDto(
    @field:NotBlank
    @field:Size(min = 32)
    @field:Schema(
        description = "Refresh token à révoquer",
        example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
        minLength = 32
    )
    val refreshToken: String,

    @field:NotNull
    @field:Schema(
        description = "Si true, révoque toutes les sessions actives de l'utilisateur",
        example = "false",
        defaultValue = "false"
    )
    val revokeAllSessions: Boolean = false
)

@Schema(description = "Réponse contenant un couple de jetons JWT (access token et refresh token)")
open class TokenPairResponseDto(
    @field:Schema(description = "Jeton d'accès JWT", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    val accessToken: String,
    
    @field:Schema(description = "Refresh token pour renouveler les jetons", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    val refreshToken: String,
    
    @field:Schema(description = "Date d'expiration du jeton d'accès (ISO 8601)", example = "2025-01-27T10:15:00Z")
    val expiresAt: Instant,
    
    @field:Schema(description = "Date d'expiration du refresh token (ISO 8601)", example = "2025-02-03T10:00:00Z")
    val refreshExpiresAt: Instant
)

typealias LoginResponseDto = TokenPairResponseDto

@Schema(description = "Réponse de validation d'un jeton JWT")
data class ValidateTokenResponseDto(
    @field:Schema(description = "Indique si le jeton est valide", example = "true")
    val valid: Boolean,
    
    @field:Schema(description = "Subject du jeton (identifiant utilisateur) si valide", example = "11111111-2222-3333-4444-555555555555", nullable = true)
    val subject: String? = null,
    
    @field:Schema(description = "Date d'expiration du jeton si valide (ISO 8601)", example = "2025-01-27T10:15:00Z", nullable = true)
    val expiresAt: Instant? = null
)

