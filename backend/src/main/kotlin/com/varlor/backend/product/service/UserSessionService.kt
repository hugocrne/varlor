package com.varlor.backend.product.service

import com.varlor.backend.common.service.BaseCrudService
import com.varlor.backend.product.model.dto.CreateUserSessionDto
import com.varlor.backend.product.model.dto.UpdateUserSessionDto
import com.varlor.backend.product.model.dto.UserSessionDto
import com.varlor.backend.product.model.entity.UserSession
import com.varlor.backend.product.repository.UserRepository
import com.varlor.backend.product.repository.UserSessionRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserSessionService(
    userSessionRepository: UserSessionRepository,
    private val userRepository: UserRepository,
    clock: Clock = Clock.systemUTC()
) : BaseCrudService<UserSession, UserSessionDto, CreateUserSessionDto, UpdateUserSessionDto, UUID>(
    repository = userSessionRepository,
    clock = clock
) {

    override fun findAll(): List<UserSessionDto> {
        return (repository as UserSessionRepository)
            .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .map(::toDto)
    }

    override fun toDto(entity: UserSession): UserSessionDto {
        return UserSessionDto(
            id = entity.id!!,
            userId = entity.userId!!,
            tokenId = entity.tokenId,
            ipAddress = entity.ipAddress,
            userAgent = entity.userAgent,
            createdAt = entity.createdAt,
            expiresAt = entity.expiresAt,
            revokedAt = entity.revokedAt,
            replacedByTokenId = entity.replacedByTokenId,
            revocationReason = entity.revocationReason
        )
    }

    override fun toEntity(dto: CreateUserSessionDto, createdAt: Instant): UserSession {
        return UserSession(
            userId = dto.userId,
            tokenId = dto.tokenId,
            tokenHash = dto.tokenHash,
            ipAddress = dto.ipAddress,
            userAgent = dto.userAgent,
            expiresAt = dto.expiresAt
        ).apply {
            this.createdAt = createdAt
            this.revokedAt = null
            this.replacedByTokenId = null
            this.revocationReason = null
        }
    }

    override fun updateEntity(entity: UserSession, dto: UpdateUserSessionDto) {
        dto.userId?.let { entity.userId = it }
        dto.tokenId?.let { entity.tokenId = it }
        dto.tokenHash?.let { entity.tokenHash = it }
        dto.ipAddress?.let { entity.ipAddress = it }
        dto.userAgent?.let { entity.userAgent = it }
        dto.expiresAt?.let { entity.expiresAt = it }
        dto.revokedAt?.let { entity.revokedAt = it }
        entity.replacedByTokenId = dto.replacedByTokenId
        entity.revocationReason = dto.revocationReason
    }

    override fun validateBeforeCreate(dto: CreateUserSessionDto) {
        val now = Instant.now(clock)
        if (!dto.expiresAt.isAfter(now)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La date d'expiration doit être dans le futur.")
        }

        ensureUserExists(dto.userId)

        val sessionRepo = repository as UserSessionRepository
        if (sessionRepo.findByTokenId(dto.tokenId).isPresent) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Un jeton de session identique existe déjà.")
        }

        if (sessionRepo.findByTokenHash(dto.tokenHash).isPresent) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Un jeton de session équivalent existe déjà.")
        }
    }

    override fun validateBeforeUpdate(id: UUID, dto: UpdateUserSessionDto, entity: UserSession) {
        if (dto.userId == null &&
            dto.tokenId == null &&
            dto.tokenHash == null &&
            dto.ipAddress == null &&
            dto.userAgent == null &&
            dto.expiresAt == null &&
            dto.revokedAt == null &&
            dto.replacedByTokenId == null &&
            dto.revocationReason == null
        ) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune donnée de mise à jour fournie.")
        }

        dto.userId?.let { ensureUserExists(it) }

        val sessionRepo = repository as UserSessionRepository
        dto.tokenId?.let { tokenId ->
            val conflict = sessionRepo.findByTokenId(tokenId)
            if (conflict.isPresent && conflict.get().id != entity.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Un jeton de session identique existe déjà.")
            }
        }

        dto.tokenHash?.let { tokenHash ->
            val conflict = sessionRepo.findByTokenHash(tokenHash)
            if (conflict.isPresent && conflict.get().id != entity.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Un jeton de session équivalent existe déjà.")
            }
        }

        dto.expiresAt?.let { expiresAt ->
            if (!expiresAt.isAfter(Instant.now(clock))) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La date d'expiration doit être dans le futur.")
            }
        }
    }

    override fun notFoundException(id: UUID): ResponseStatusException =
        ResponseStatusException(HttpStatus.NOT_FOUND, "Session utilisateur introuvable.")

    private fun ensureUserExists(userId: UUID) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur associé introuvable ou supprimé.")
        }
    }
}

