package com.varlor.backend.product.repository

import com.varlor.backend.product.model.entity.UserPreference
import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserPreferenceRepository : JpaRepository<UserPreference, UUID> {
    fun findByUserId(userId: UUID): Optional<UserPreference>

    fun existsByUserId(userId: UUID): Boolean
}

