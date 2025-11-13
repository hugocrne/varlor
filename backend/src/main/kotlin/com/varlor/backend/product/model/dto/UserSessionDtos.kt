package com.varlor.backend.product.model.dto

import com.varlor.backend.common.model.BaseDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "DTO pour la création d'une session utilisateur")
data class CreateUserSessionDto(
    @field:NotNull
    @field:Schema(description = "Identifiant de l'utilisateur propriétaire de la session", example = "11111111-2222-3333-4444-555555555555")
    val userId: UUID,

    @field:NotBlank
    @field:Size(max = 255)
    @field:Schema(description = "Identifiant unique du token", example = "abc123def456", maxLength = 255)
    val tokenId: String,

    @field:NotBlank
    @field:Size(max = 128)
    @field:Schema(description = "Hash SHA-256 du refresh token", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", maxLength = 128)
    val tokenHash: String,

    @field:NotBlank
    @field:Size(max = 45)
    @field:Schema(description = "Adresse IP du client", example = "192.168.1.1", maxLength = 45)
    val ipAddress: String,

    @field:NotBlank
    @field:Size(max = 500)
    @field:Schema(description = "User-Agent du client", example = "Mozilla/5.0...", maxLength = 500)
    val userAgent: String,

    @field:NotNull
    @field:Schema(description = "Date d'expiration de la session (ISO 8601)", example = "2025-02-03T10:00:00Z")
    val expiresAt: Instant
)

@Schema(description = "DTO pour la mise à jour d'une session utilisateur (tous les champs sont optionnels)")
data class UpdateUserSessionDto(
    @field:Schema(description = "Nouvel identifiant utilisateur", example = "11111111-2222-3333-4444-555555555555", nullable = true)
    val userId: UUID? = null,

    @field:Size(max = 255)
    @field:Schema(description = "Nouvel identifiant de token", example = "abc123def456", nullable = true, maxLength = 255)
    val tokenId: String? = null,

    @field:Size(max = 128)
    @field:Schema(description = "Nouveau hash de token", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", nullable = true, maxLength = 128)
    val tokenHash: String? = null,

    @field:Size(max = 45)
    @field:Schema(description = "Nouvelle adresse IP", example = "192.168.1.1", nullable = true, maxLength = 45)
    val ipAddress: String? = null,

    @field:Size(max = 500)
    @field:Schema(description = "Nouveau User-Agent", example = "Mozilla/5.0...", nullable = true, maxLength = 500)
    val userAgent: String? = null,

    @field:Schema(description = "Nouvelle date d'expiration (ISO 8601)", example = "2025-02-03T10:00:00Z", nullable = true)
    val expiresAt: Instant? = null,

    @field:Schema(description = "Date de révocation (ISO 8601)", example = "2025-01-27T10:00:00Z", nullable = true)
    val revokedAt: Instant? = null,

    @field:Size(max = 255)
    @field:Schema(description = "Identifiant du token de remplacement (rotation)", example = "xyz789", nullable = true, maxLength = 255)
    val replacedByTokenId: String? = null,

    @field:Size(max = 255)
    @field:Schema(description = "Raison de la révocation", example = "LOGOUT", nullable = true, maxLength = 255)
    val revocationReason: String? = null
)

@Schema(description = "DTO représentant une session utilisateur avec ses métadonnées")
data class UserSessionDto(
    @field:Schema(description = "Identifiant unique de la session", example = "11111111-2222-3333-4444-555555555555")
    override val id: UUID,
    
    @field:Schema(description = "Identifiant de l'utilisateur propriétaire", example = "11111111-2222-3333-4444-555555555555")
    val userId: UUID,
    
    @field:Schema(description = "Identifiant unique du token", example = "abc123def456")
    val tokenId: String,
    
    @field:Schema(description = "Adresse IP du client", example = "192.168.1.1")
    val ipAddress: String,
    
    @field:Schema(description = "User-Agent du client", example = "Mozilla/5.0...")
    val userAgent: String,
    
    @field:Schema(description = "Date de création (ISO 8601)", example = "2025-01-27T10:00:00Z", nullable = true)
    override val createdAt: Instant?,
    
    @field:Schema(description = "Date de dernière mise à jour (ISO 8601)", example = "2025-01-27T10:00:00Z", nullable = true)
    override val updatedAt: Instant?,
    
    @field:Schema(description = "Date d'expiration de la session (ISO 8601)", example = "2025-02-03T10:00:00Z")
    val expiresAt: Instant,
    
    @field:Schema(description = "Date de révocation si révoquée (ISO 8601)", example = "2025-01-27T10:00:00Z", nullable = true)
    val revokedAt: Instant?,
    
    @field:Schema(description = "Identifiant du token de remplacement (rotation)", example = "xyz789", nullable = true)
    val replacedByTokenId: String?,
    
    @field:Schema(description = "Raison de la révocation", example = "LOGOUT", nullable = true)
    val revocationReason: String?
) : BaseDto<UUID>

