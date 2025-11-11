package com.varlor.backend.product.model.dto

import com.varlor.backend.product.model.entity.UserRole
import com.varlor.backend.product.model.entity.UserStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateUserDto(
    @field:NotNull
    val clientId: UUID,

    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,

    @field:NotBlank
    @field:Size(max = 255)
    val passwordHash: String,

    @field:NotBlank
    @field:Size(max = 100)
    val firstName: String,

    @field:NotBlank
    @field:Size(max = 100)
    val lastName: String,

    @field:NotNull
    val role: UserRole,

    @field:NotNull
    val status: UserStatus
)

data class UpdateUserDto(
    val clientId: UUID? = null,

    @field:Email
    @field:Size(max = 255)
    val email: String? = null,

    @field:Size(max = 255)
    val passwordHash: String? = null,

    @field:Size(max = 100)
    val firstName: String? = null,

    @field:Size(max = 100)
    val lastName: String? = null,

    val role: UserRole? = null,

    val status: UserStatus? = null,

    val lastLoginAt: Instant? = null,

    val deletedAt: Instant? = null
)

data class UserDto(
    val id: UUID,
    val clientId: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val status: UserStatus,
    val lastLoginAt: Instant?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val deletedAt: Instant?
)

