package com.varlor.backend.product.model.dto

import com.varlor.backend.product.model.entity.Theme
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateUserPreferenceDto(
    @field:NotNull
    val userId: UUID,

    @field:NotNull
    val theme: Theme,

    @field:NotBlank
    @field:Size(max = 10)
    val language: String,

    @field:NotNull
    val notificationsEnabled: Boolean
)

data class UpdateUserPreferenceDto(
    val theme: Theme? = null,

    @field:Size(max = 10)
    val language: String? = null,

    val notificationsEnabled: Boolean? = null
)

data class UserPreferenceDto(
    val id: UUID,
    val userId: UUID,
    val theme: Theme,
    val language: String,
    val notificationsEnabled: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

