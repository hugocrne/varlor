package com.varlor.backend.product.service

import com.varlor.backend.common.service.BaseCrudService
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
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(
    userRepository: UserRepository,
    private val clientRepository: ClientRepository,
    private val passwordEncoder: PasswordEncoder,
    clock: Clock = Clock.systemUTC()
) : BaseCrudService<User, UserDto, CreateUserDto, UpdateUserDto, UUID>(
    repository = userRepository,
    clock = clock
) {

    override fun supportsSoftDelete(): Boolean = true

    override fun toDto(entity: User): UserDto {
        return UserDto(
            id = entity.id!!,
            clientId = entity.clientId!!,
            email = entity.email,
            firstName = entity.firstName,
            lastName = entity.lastName,
            role = entity.role,
            status = entity.status,
            lastLoginAt = entity.lastLoginAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt
        )
    }

    override fun toEntity(dto: CreateUserDto, createdAt: Instant): User {
        return User(
            clientId = dto.clientId,
            email = dto.email.lowercase(Locale.getDefault()),
            passwordHash = passwordEncoder.encode(dto.password),
            firstName = dto.firstName,
            lastName = dto.lastName,
            role = dto.role,
            status = dto.status
        ).apply {
            this.createdAt = createdAt
            this.updatedAt = createdAt
            this.lastLoginAt = null
            this.deletedAt = null
        }
    }

    override fun updateEntity(entity: User, dto: UpdateUserDto) {
        dto.clientId?.let {
            entity.clientId = it
        }

        dto.email?.let {
            val normalized = it.lowercase(Locale.getDefault())
            entity.email = normalized
        }

        dto.password?.let {
            entity.passwordHash = passwordEncoder.encode(it)
        }

        dto.firstName?.let { entity.firstName = it }
        dto.lastName?.let { entity.lastName = it }
        dto.role?.let { entity.role = it }
        dto.status?.let { entity.status = it }
        dto.lastLoginAt?.let { entity.lastLoginAt = it }
        dto.deletedAt?.let { entity.deletedAt = it }
    }

    override fun validateBeforeCreate(dto: CreateUserDto) {
        ensureClientExists(dto.clientId)

        val existing = (repository as UserRepository).findByEmailAndDeletedAtIsNull(dto.email)
        if (existing != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur avec cet email existe déjà.")
        }
    }

    override fun validateBeforeUpdate(id: UUID, dto: UpdateUserDto, entity: User) {
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

        dto.clientId?.let { ensureClientExists(it) }

        dto.email?.let {
            val normalized = it.lowercase(Locale.getDefault())
            val conflict = (repository as UserRepository).findByEmailAndDeletedAtIsNull(normalized)
            if (conflict != null && conflict.id != entity.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur avec cet email existe déjà.")
            }
        }
    }

    override fun notFoundException(id: UUID): ResponseStatusException =
        ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable.")

    override fun performSoftDelete(entity: User) {
        super.performSoftDelete(entity)
        entity.status = UserStatus.INACTIVE
    }

    private fun ensureClientExists(clientId: UUID) {
        if (!clientRepository.existsByIdAndDeletedAtIsNull(clientId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Client associé introuvable ou supprimé.")
        }
    }
}

