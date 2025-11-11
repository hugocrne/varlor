package com.varlor.backend.product.service

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
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class UserPreferenceService(
    private val userPreferenceRepository: UserPreferenceRepository,
    private val userRepository: UserRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun findAll(): List<UserPreferenceDto> {
        return userPreferenceRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).map(::toDto)
    }

    fun findById(id: UUID): UserPreferenceDto {
        val preference = userPreferenceRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Préférence utilisateur introuvable.") }
        return toDto(preference)
    }

    @Transactional
    fun create(dto: CreateUserPreferenceDto): UserPreferenceDto {
        if (!userRepository.existsByIdAndDeletedAtIsNull(dto.userId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur associé introuvable ou supprimé.")
        }

        if (userPreferenceRepository.existsByUserId(dto.userId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Une préférence existe déjà pour cet utilisateur.")
        }

        val now = Instant.now(clock)
        val preference = UserPreference(
            userId = dto.userId,
            theme = dto.theme,
            language = dto.language,
            notificationsEnabled = dto.notificationsEnabled
        ).apply {
            createdAt = now
            updatedAt = now
        }

        val saved = userPreferenceRepository.save(preference)
        return toDto(saved)
    }

    @Transactional
    fun update(id: UUID, dto: UpdateUserPreferenceDto): UserPreferenceDto {
        if (dto.theme == null && dto.language == null && dto.notificationsEnabled == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune donnée de mise à jour fournie.")
        }

        val preference = userPreferenceRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Préférence utilisateur introuvable.") }

        dto.theme?.let { preference.theme = it }
        dto.language?.let { preference.language = it }
        dto.notificationsEnabled?.let { preference.notificationsEnabled = it }

        preference.updatedAt = Instant.now(clock)

        return toDto(preference)
    }

    @Transactional
    fun delete(id: UUID) {
        val preference = userPreferenceRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Préférence utilisateur introuvable.") }
        userPreferenceRepository.delete(preference)
    }

    private fun toDto(preference: UserPreference): UserPreferenceDto {
        return UserPreferenceDto(
            id = preference.id!!,
            userId = preference.userId!!,
            theme = preference.theme,
            language = preference.language,
            notificationsEnabled = preference.notificationsEnabled,
            createdAt = preference.createdAt,
            updatedAt = preference.updatedAt
        )
    }
}

