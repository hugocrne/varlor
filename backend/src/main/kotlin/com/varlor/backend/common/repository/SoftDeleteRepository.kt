package com.varlor.backend.common.repository

import com.varlor.backend.common.model.SoftDeletableEntity
import java.io.Serializable
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Interface marker pour les repositories qui supportent le soft delete.
 * Les méthodes de requête doivent être définies dans les repositories concrets
 * car Spring Data JPA ne peut pas créer de requêtes pour les types génériques abstraits.
 */
interface SoftDeleteRepository<ENTITY : SoftDeletableEntity<ID>, ID : Serializable> 
    : JpaRepository<ENTITY, ID>

/**
 * Interface avec les méthodes de soft delete.
 * Les repositories concrets doivent implémenter cette interface en plus de SoftDeleteRepository.
 */
interface SoftDeleteRepositoryMethods<ENTITY, ID : Serializable> {
    fun findByIdAndDeletedAtIsNull(id: ID): java.util.Optional<ENTITY>
    fun findAllByDeletedAtIsNull(): List<ENTITY>
    fun existsByIdAndDeletedAtIsNull(id: ID): Boolean
}

