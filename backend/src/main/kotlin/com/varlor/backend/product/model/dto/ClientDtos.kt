package com.varlor.backend.product.model.dto

import com.varlor.backend.common.model.SoftDeletableDto
import com.varlor.backend.product.model.entity.ClientStatus
import com.varlor.backend.product.model.entity.ClientType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateClientDto(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    @field:NotNull
    val type: ClientType,

    @field:NotNull
    val status: ClientStatus
)

data class UpdateClientDto(
    @field:Size(max = 255)
    val name: String? = null,
    val type: ClientType? = null,
    val status: ClientStatus? = null
)

data class ClientDto(
    override val id: UUID,
    val name: String,
    val type: ClientType,
    val status: ClientStatus,
    override val createdAt: Instant?,
    override val updatedAt: Instant?,
    override val deletedAt: Instant?
) : SoftDeletableDto<UUID>

