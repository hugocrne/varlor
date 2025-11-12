package com.varlor.backend.product.model.dto

import com.varlor.backend.product.model.entity.UserRole
import com.varlor.backend.product.model.entity.UserStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequestDto(
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
    val password: String,

    @field:NotBlank
    @field:Size(max = 100)
    val firstName: String,

    @field:NotBlank
    @field:Size(max = 100)
    val lastName: String,

    val role: UserRole = UserRole.MEMBER,
    val status: UserStatus = UserStatus.ACTIVE
)

data class LoginRequestDto(
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,

    @field:NotBlank
    @field:Size(max = 255)
    val password: String
)

data class RefreshTokenRequestDto(
    @field:NotBlank
    @field:Size(min = 32)
    val refreshToken: String
)

data class LogoutRequestDto(
    @field:NotBlank
    @field:Size(min = 32)
    val refreshToken: String,

    @field:NotNull
    val revokeAllSessions: Boolean = false
)

open class TokenPairResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val refreshExpiresAt: Instant
)

typealias LoginResponseDto = TokenPairResponseDto

data class ValidateTokenResponseDto(
    val valid: Boolean,
    val subject: String? = null,
    val expiresAt: Instant? = null
)

