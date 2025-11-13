package com.varlor.backend.product.model.dto

import com.varlor.backend.common.model.BaseDto
import com.varlor.backend.product.model.entity.Theme
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "DTO pour la création de préférences utilisateur")
data class CreateUserPreferenceDto(
    @field:NotNull
    @field:Schema(description = "Identifiant de l'utilisateur propriétaire des préférences", example = "11111111-2222-3333-4444-555555555555")
    val userId: UUID,

    @field:NotNull
    @field:Schema(description = "Thème d'affichage", example = "LIGHT")
    val theme: Theme,

    @field:NotBlank
    @field:Size(max = 10)
    @field:Schema(description = "Code langue (ISO 639-1)", example = "fr", maxLength = 10)
    val language: String,

    @field:NotNull
    @field:Schema(description = "Activation des notifications", example = "true")
    val notificationsEnabled: Boolean
)

@Schema(description = "DTO pour la mise à jour de préférences utilisateur (tous les champs sont optionnels)")
data class UpdateUserPreferenceDto(
    @field:Schema(description = "Nouveau thème d'affichage", example = "DARK", nullable = true)
    val theme: Theme? = null,

    @field:Size(max = 10)
    @field:Schema(description = "Nouveau code langue (ISO 639-1)", example = "en", nullable = true, maxLength = 10)
    val language: String? = null,

    @field:Schema(description = "Nouvelle activation des notifications", example = "false", nullable = true)
    val notificationsEnabled: Boolean? = null
)

@Schema(description = "DTO représentant les préférences d'un utilisateur")
data class UserPreferenceDto(
    @field:Schema(description = "Identifiant unique des préférences", example = "11111111-2222-3333-4444-555555555555")
    override val id: UUID,
    
    @field:Schema(description = "Identifiant de l'utilisateur propriétaire", example = "11111111-2222-3333-4444-555555555555")
    val userId: UUID,
    
    @field:Schema(description = "Thème d'affichage", example = "LIGHT")
    val theme: Theme,
    
    @field:Schema(description = "Code langue (ISO 639-1)", example = "fr")
    val language: String,
    
    @field:Schema(description = "Activation des notifications", example = "true")
    val notificationsEnabled: Boolean,
    
    @field:Schema(description = "Date de création (ISO 8601)", example = "2025-01-27T10:00:00Z", nullable = true)
    override val createdAt: Instant?,
    
    @field:Schema(description = "Date de dernière mise à jour (ISO 8601)", example = "2025-01-27T10:00:00Z", nullable = true)
    override val updatedAt: Instant?
) : BaseDto<UUID>

