package com.varlor.backend.product.service

import com.varlor.backend.product.model.dto.CreateUserDto
import com.varlor.backend.product.model.dto.UpdateUserDto
import com.varlor.backend.product.model.dto.UserDto
import com.varlor.backend.product.model.entity.User
import com.varlor.backend.product.model.entity.UserStatus
import com.varlor.backend.product.repository.ClientRepository
import com.varlor.backend.product.repository.UserRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.Locale
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val clientRepository: ClientRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock = Clock.systemUTC()
) {

    fun findAll(): List<UserDto> {
        return userRepository.findAllByDeletedAtIsNull().map(::toDto)
    }

    fun findById(id: UUID): UserDto {
        val user = userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable.") }
        return toDto(user)
    }

    @Transactional
    fun create(dto: CreateUserDto): UserDto {
        ensureClientExists(dto.clientId)

        val existing = userRepository.findByEmailAndDeletedAtIsNull(dto.email)
        if (existing != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur avec cet email existe déjà.")
        }

        val now = Instant.now(clock)
        val user = User(
            clientId = dto.clientId,
            email = dto.email.lowercase(Locale.getDefault()),
            passwordHash = passwordEncoder.encode(dto.password),
            firstName = dto.firstName,
            lastName = dto.lastName,
            role = dto.role,
            status = dto.status
        ).apply {
            createdAt = now
            updatedAt = now
            lastLoginAt = null
            deletedAt = null
        }

        val saved = userRepository.save(user)
        return toDto(saved)
    }

    @Transactional
    fun update(id: UUID, dto: UpdateUserDto): UserDto {
        if (dto.clientId == null &&
            dto.email == null &&
            dto.password == null &&
            dto.firstName == null &&
            dto.lastName == null &&
            dto.role == null &&
            dto.status == null &&
            dto.lastLoginAt == null &&
            dto.deletedAt == null
        ) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune donnée de mise à jour fournie.")
        }

        val user = userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable.") }

        dto.clientId?.let {
            ensureClientExists(it)
            user.clientId = it
        }

        dto.email?.let {
            val normalized = it.lowercase(Locale.getDefault())
            val conflict = userRepository.findByEmailAndDeletedAtIsNull(normalized)
            if (conflict != null && conflict.id != user.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur avec cet email existe déjà.")
            }
            user.email = normalized
        }

        dto.password?.let {
            user.passwordHash = passwordEncoder.encode(it)
        }

        dto.firstName?.let { user.firstName = it }
        dto.lastName?.let { user.lastName = it }
        dto.role?.let { user.role = it }
        dto.status?.let { user.status = it }
        dto.lastLoginAt?.let { user.lastLoginAt = it }
        dto.deletedAt?.let { user.deletedAt = it }

        user.updatedAt = Instant.now(clock)

        return toDto(user)
    }

    /**
     * Applique un soft delete : l'utilisateur est marqué INACTIVE et conservé en base.
     */
    @Transactional
    fun delete(id: UUID) {
        val user = userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable.") }

        val now = Instant.now(clock)
        user.status = UserStatus.INACTIVE
        user.deletedAt = now
        user.updatedAt = now
    }

    private fun ensureClientExists(clientId: UUID) {
        if (!clientRepository.existsByIdAndDeletedAtIsNull(clientId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Client associé introuvable ou supprimé.")
        }
    }

    private fun toDto(user: User): UserDto {
        return UserDto(
            id = user.id!!,
            clientId = user.clientId!!,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role,
            status = user.status,
            lastLoginAt = user.lastLoginAt,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            deletedAt = user.deletedAt
        )
    }
}

