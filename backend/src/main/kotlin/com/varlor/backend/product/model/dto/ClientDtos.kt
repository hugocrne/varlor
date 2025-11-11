package com.varlor.backend.product.model.dto

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
    val id: UUID,
    val name: String,
    val type: ClientType,
    val status: ClientStatus,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val deletedAt: Instant?
)

