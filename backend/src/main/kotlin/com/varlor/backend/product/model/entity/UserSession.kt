package com.varlor.backend.product.model.entity

import com.varlor.backend.common.model.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_sessions")
class UserSession(
    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,

    @Column(name = "token_id", nullable = false, length = 255)
    var tokenId: String = "",

    @Column(name = "token_hash", nullable = false, length = 128)
    var tokenHash: String = "",

    @Column(name = "ip_address", nullable = false, length = 45)
    var ipAddress: String = "",

    @Column(name = "user_agent", nullable = false, length = 500)
    var userAgent: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "replaced_by_token_id", length = 255)
    var replacedByTokenId: String? = null,

    @Column(name = "revocation_reason", length = 255)
    var revocationReason: String? = null
) : BaseEntity<UUID>() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    var user: User? = null
}

