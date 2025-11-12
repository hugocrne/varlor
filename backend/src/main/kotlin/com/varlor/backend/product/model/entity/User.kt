package com.varlor.backend.product.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.varlor.backend.common.model.SoftDeletableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Column(name = "client_id", nullable = false)
    var clientId: UUID? = null,

    @Column(name = "email", nullable = false, length = 255, unique = true)
    var email: String = "",

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String = "",

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String = "",

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    var role: UserRole = UserRole.MEMBER,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: UserStatus = UserStatus.PENDING,

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null
) : SoftDeletableEntity<UUID>() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, insertable = false, updatable = false)
    var client: Client? = null

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    var userPreference: UserPreference? = null

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    var userSessions: MutableSet<UserSession> = LinkedHashSet()
}

