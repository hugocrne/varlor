package com.varlor.backend.product.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.varlor.backend.common.model.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_preferences")
class UserPreference(
    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false, length = 50)
    var theme: Theme = Theme.SYSTEM,

    @Column(name = "language", nullable = false, length = 10)
    var language: String = "en",

    @Column(name = "notifications_enabled", nullable = false)
    var notificationsEnabled: Boolean = true
) : BaseEntity<UUID>() {
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    var user: User? = null
}

