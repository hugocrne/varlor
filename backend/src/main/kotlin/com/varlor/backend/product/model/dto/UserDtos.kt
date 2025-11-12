package com.varlor.backend.product.model.dto

import com.varlor.backend.common.model.SoftDeletableDto
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

data class CreateUserDto(
    @field:NotNull
    val clientId: UUID,

    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 255)
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial (@$!%*?&)"
    )
    @field:Schema(
        description = "Mot de passe en clair soumis par l'utilisateur, haché côté serveur avant stockage.",
        example = "Secret123!"
    )
    val password: String,

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

    @field:Size(min = 8, max = 255)
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial (@$!%*?&)"
    )
    @field:Schema(
        description = "Nouveau mot de passe en clair, haché côté serveur avant stockage.",
        example = "Secret123!",
        nullable = true
    )
    val password: String? = null,

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
    override val id: UUID,
    val clientId: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val status: UserStatus,
    val lastLoginAt: Instant?,
    override val createdAt: Instant?,
    override val updatedAt: Instant?,
    override val deletedAt: Instant?
) : SoftDeletableDto<UUID>

