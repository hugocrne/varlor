package com.varlor.backend.product.model.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateUserSessionDto(
    @field:NotNull
    val userId: UUID,

    @field:NotBlank
    @field:Size(max = 255)
    val tokenId: String,

    @field:NotBlank
    @field:Size(max = 128)
    val tokenHash: String,

    @field:NotBlank
    @field:Size(max = 45)
    val ipAddress: String,

    @field:NotBlank
    @field:Size(max = 500)
    val userAgent: String,

    @field:NotNull
    val expiresAt: Instant
)

data class UpdateUserSessionDto(
    val userId: UUID? = null,

    @field:Size(max = 255)
    val tokenId: String? = null,

    @field:Size(max = 128)
    val tokenHash: String? = null,

    @field:Size(max = 45)
    val ipAddress: String? = null,

    @field:Size(max = 500)
    val userAgent: String? = null,

    val expiresAt: Instant? = null,

    val revokedAt: Instant? = null,

    @field:Size(max = 255)
    val replacedByTokenId: String? = null,

    @field:Size(max = 255)
    val revocationReason: String? = null
)

data class UserSessionDto(
    val id: UUID,
    val userId: UUID,
    val tokenId: String,
    val ipAddress: String,
    val userAgent: String,
    val createdAt: Instant?,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val replacedByTokenId: String?,
    val revocationReason: String?
)

