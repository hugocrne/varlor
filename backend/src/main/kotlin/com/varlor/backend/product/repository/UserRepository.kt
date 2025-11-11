package com.varlor.backend.product.repository

import com.varlor.backend.product.model.entity.User
import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmailAndDeletedAtIsNull(email: String): User?

    fun findByIdAndDeletedAtIsNull(id: UUID): Optional<User>

    fun findAllByDeletedAtIsNull(): List<User>

    fun existsByIdAndDeletedAtIsNull(id: UUID): Boolean
}

