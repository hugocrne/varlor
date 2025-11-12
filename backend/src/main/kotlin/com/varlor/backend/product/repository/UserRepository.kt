package com.varlor.backend.product.repository

import com.varlor.backend.common.repository.SoftDeleteRepository
import com.varlor.backend.common.repository.SoftDeleteRepositoryMethods
import com.varlor.backend.product.model.entity.User
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : SoftDeleteRepository<User, UUID>, SoftDeleteRepositoryMethods<User, UUID> {
    fun findByEmailAndDeletedAtIsNull(email: String): User?
}

