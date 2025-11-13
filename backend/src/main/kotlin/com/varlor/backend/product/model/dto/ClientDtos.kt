package com.varlor.backend.product.model.dto

import com.varlor.backend.common.model.SoftDeletableDto
import com.varlor.backend.product.model.entity.ClientStatus
import com.varlor.backend.product.model.entity.ClientType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "DTO pour la création d'un client")
data class CreateClientDto(
    @field:NotBlank
    @field:Size(max = 255)
    @field:Schema(description = "Nom du client", example = "Acme Corporation", maxLength = 255)
    val name: String,

    @field:NotNull
    @field:Schema(description = "Type de client", example = "ENTERPRISE")
    val type: ClientType,

    @field:NotNull
    @field:Schema(description = "Statut initial du client", example = "ACTIVE")
    val status: ClientStatus
)

@Schema(description = "DTO pour la mise à jour d'un client (tous les champs sont optionnels)")
data class UpdateClientDto(
    @field:Size(max = 255)
    @field:Schema(description = "Nouveau nom du client", example = "Acme Corporation Updated", nullable = true, maxLength = 255)
    val name: String? = null,
    
    @field:Schema(description = "Nouveau type de client", example = "ENTERPRISE", nullable = true)
    val type: ClientType? = null,
    
    @field:Schema(description = "Nouveau statut du client", example = "ACTIVE", nullable = true)
    val status: ClientStatus? = null
)

@Schema(description = "DTO représentant un client avec ses métadonnées")
data class ClientDto(
    @field:Schema(description = "Identifiant unique du client", example = "11111111-2222-3333-4444-555555555555")
    override val id: UUID,
    
    @field:Schema(description = "Nom du client", example = "Acme Corporation")
    val name: String,
    
    @field:Schema(description = "Type de client", example = "ENTERPRISE")
    val type: ClientType,
    
    @field:Schema(description = "Statut du client", example = "ACTIVE")
    val status: ClientStatus,
    
    @field:Schema(description = "Date de création (ISO 8601)", example = "2025-01-27T10:00:00Z", nullable = true)
    override val createdAt: Instant?,
    
    @field:Schema(description = "Date de dernière mise à jour (ISO 8601)", example = "2025-01-27T10:00:00Z", nullable = true)
    override val updatedAt: Instant?,
    
    @field:Schema(description = "Date de suppression logique (ISO 8601)", example = "2025-01-27T10:00:00Z", nullable = true)
    override val deletedAt: Instant?
) : SoftDeletableDto<UUID>

