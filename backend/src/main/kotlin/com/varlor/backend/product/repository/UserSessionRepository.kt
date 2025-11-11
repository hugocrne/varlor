package com.varlor.backend.product.repository

import com.varlor.backend.product.model.entity.UserSession
import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserSessionRepository : JpaRepository<UserSession, UUID> {
    fun findByTokenHash(tokenHash: String): Optional<UserSession>

    fun findByTokenId(tokenId: String): Optional<UserSession>

    fun findAllByUserIdAndRevokedAtIsNull(userId: UUID): List<UserSession>

    fun findAllByUserIdAndExpiresAtLessThanEqual(userId: UUID, expiresAt: Instant): List<UserSession>

    fun deleteAllByUserIdAndExpiresAtLessThanEqual(userId: UUID, expiresAt: Instant): Long
}

