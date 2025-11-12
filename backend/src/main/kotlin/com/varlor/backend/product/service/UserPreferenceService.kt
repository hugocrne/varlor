package com.varlor.backend.product.service

import com.varlor.backend.common.extensions.requireAtLeastOneField
import com.varlor.backend.common.extensions.requireExists
import com.varlor.backend.common.repository.SoftDeleteRepositoryMethods
import com.varlor.backend.common.service.BaseCrudService
import com.varlor.backend.product.model.dto.CreateUserPreferenceDto
import com.varlor.backend.product.model.dto.UpdateUserPreferenceDto
import com.varlor.backend.product.model.dto.UserPreferenceDto
import com.varlor.backend.product.model.entity.UserPreference
import com.varlor.backend.product.repository.UserPreferenceRepository
import com.varlor.backend.product.repository.UserRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserPreferenceService(
    userPreferenceRepository: UserPreferenceRepository,
    private val userRepository: UserRepository,
    clock: Clock = Clock.systemUTC()
) : BaseCrudService<UserPreference, UserPreferenceDto, CreateUserPreferenceDto, UpdateUserPreferenceDto, UUID>(
    repository = userPreferenceRepository,
    clock = clock
) {

    override fun findAll(): List<UserPreferenceDto> {
        return (repository as UserPreferenceRepository)
            .findAll(Sort.by(Sort.Direction.ASC, "createdAt"))
            .map(::toDto)
    }

    override fun toDto(entity: UserPreference): UserPreferenceDto {
        return UserPreferenceDto(
            id = entity.id!!,
            userId = entity.userId!!,
            theme = entity.theme,
            language = entity.language,
            notificationsEnabled = entity.notificationsEnabled,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(dto: CreateUserPreferenceDto, createdAt: Instant): UserPreference {
        return UserPreference(
            userId = dto.userId,
            theme = dto.theme,
            language = dto.language,
            notificationsEnabled = dto.notificationsEnabled
        ).apply {
            this.createdAt = createdAt
            this.updatedAt = createdAt
        }
    }

    override fun updateEntity(entity: UserPreference, dto: UpdateUserPreferenceDto) {
        dto.theme?.let { entity.theme = it }
        dto.language?.let { entity.language = it }
        dto.notificationsEnabled?.let { entity.notificationsEnabled = it }
    }

    override fun validateBeforeCreate(dto: CreateUserPreferenceDto) {
        (userRepository as SoftDeleteRepositoryMethods<*, UUID>)
            .requireExists(dto.userId, "Utilisateur")

        if ((repository as UserPreferenceRepository).existsByUserId(dto.userId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Une préférence existe déjà pour cet utilisateur.")
        }
    }

    override fun validateBeforeUpdate(id: UUID, dto: UpdateUserPreferenceDto, entity: UserPreference) {
        dto.requireAtLeastOneField()
    }

    override fun notFoundException(id: UUID): ResponseStatusException =
        ResponseStatusException(HttpStatus.NOT_FOUND, "Préférence utilisateur introuvable.")
}

