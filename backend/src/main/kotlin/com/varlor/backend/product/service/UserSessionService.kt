package com.varlor.backend.product.service

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
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class UserSessionService(
    private val userSessionRepository: UserSessionRepository,
    private val userRepository: UserRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun findAll(): List<UserSessionDto> {
        return userSessionRepository
            .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .map(::toDto)
    }

    fun findById(id: UUID): UserSessionDto {
        val session = userSessionRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Session utilisateur introuvable.") }
        return toDto(session)
    }

    @Transactional
    fun create(dto: CreateUserSessionDto): UserSessionDto {
        val now = Instant.now(clock)
        if (!dto.expiresAt.isAfter(now)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La date d'expiration doit être dans le futur.")
        }

        ensureUserExists(dto.userId)

        if (userSessionRepository.findByTokenId(dto.tokenId).isPresent) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Un jeton de session identique existe déjà.")
        }

        if (userSessionRepository.findByTokenHash(dto.tokenHash).isPresent) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Un jeton de session équivalent existe déjà.")
        }

        val session = UserSession(
            userId = dto.userId,
            tokenId = dto.tokenId,
            tokenHash = dto.tokenHash,
            ipAddress = dto.ipAddress,
            userAgent = dto.userAgent,
            expiresAt = dto.expiresAt
        ).apply {
            createdAt = now
            revokedAt = null
            replacedByTokenId = null
            revocationReason = null
        }

        val saved = userSessionRepository.save(session)
        return toDto(saved)
    }

    @Transactional
    fun update(id: UUID, dto: UpdateUserSessionDto): UserSessionDto {
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

        val session = userSessionRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Session utilisateur introuvable.") }

        dto.userId?.let {
            ensureUserExists(it)
            session.userId = it
        }

        dto.tokenId?.let { tokenId ->
            val conflict = userSessionRepository.findByTokenId(tokenId)
            if (conflict.isPresent && conflict.get().id != session.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Un jeton de session identique existe déjà.")
            }
            session.tokenId = tokenId
        }

        dto.tokenHash?.let { tokenHash ->
            val conflict = userSessionRepository.findByTokenHash(tokenHash)
            if (conflict.isPresent && conflict.get().id != session.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Un jeton de session équivalent existe déjà.")
            }
            session.tokenHash = tokenHash
        }

        dto.ipAddress?.let { session.ipAddress = it }
        dto.userAgent?.let { session.userAgent = it }

        dto.expiresAt?.let { expiresAt ->
            if (!expiresAt.isAfter(Instant.now(clock))) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La date d'expiration doit être dans le futur.")
            }
            session.expiresAt = expiresAt
        }

        dto.revokedAt?.let { session.revokedAt = it }
        session.replacedByTokenId = dto.replacedByTokenId
        session.revocationReason = dto.revocationReason

        return toDto(session)
    }

    @Transactional
    fun delete(id: UUID) {
        val session = userSessionRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Session utilisateur introuvable.") }
        userSessionRepository.delete(session)
    }

    private fun ensureUserExists(userId: UUID) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur associé introuvable ou supprimé.")
        }
    }

    private fun toDto(session: UserSession): UserSessionDto {
        return UserSessionDto(
            id = session.id!!,
            userId = session.userId!!,
            tokenId = session.tokenId,
            ipAddress = session.ipAddress,
            userAgent = session.userAgent,
            createdAt = session.createdAt,
            expiresAt = session.expiresAt,
            revokedAt = session.revokedAt,
            replacedByTokenId = session.replacedByTokenId,
            revocationReason = session.revocationReason
        )
    }
}

