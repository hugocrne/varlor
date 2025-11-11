package com.varlor.backend.product.repository

import com.varlor.backend.product.model.entity.Client
import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClientRepository : JpaRepository<Client, UUID> {
    fun findByIdAndDeletedAtIsNull(id: UUID): Optional<Client>

    fun findAllByDeletedAtIsNull(): List<Client>

    fun existsByIdAndDeletedAtIsNull(id: UUID): Boolean
}

