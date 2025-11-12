package com.varlor.backend.common.service

import com.varlor.backend.common.model.BaseEntity
import com.varlor.backend.common.model.SoftDeletableEntity
import com.varlor.backend.common.repository.SoftDeleteRepository
import com.varlor.backend.common.repository.SoftDeleteRepositoryMethods
import java.io.Serializable
import java.time.Clock
import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

abstract class BaseCrudService<
    ENTITY : BaseEntity<ID>,
    DTO,
    CREATE_DTO,
    UPDATE_DTO,
    ID : Serializable
>(
    protected val repository: JpaRepository<ENTITY, ID>,
    protected val clock: Clock = Clock.systemUTC()
) : CrudService<DTO, CREATE_DTO, UPDATE_DTO, ID> {

    override fun findAll(): List<DTO> {
        return if (supportsSoftDelete()) {
            @Suppress("UNCHECKED_CAST")
            val softDeleteMethods = repository as SoftDeleteRepositoryMethods<ENTITY, ID>
            @Suppress("UNCHECKED_CAST")
            (softDeleteMethods.findAllByDeletedAtIsNull() as List<ENTITY>).map(::toDto)
        } else {
            repository.findAll().map(::toDto)
        }
    }

    override fun findById(id: ID): DTO {
        val entity = findEntityById(id)
        return toDto(entity)
    }

    @Transactional
    override fun create(dto: CREATE_DTO): DTO {
        validateBeforeCreate(dto)
        val now = Instant.now(clock)
        val entity = toEntity(dto, now)
        val saved = repository.save(entity)
        return toDto(saved)
    }

    @Transactional
    override fun update(id: ID, dto: UPDATE_DTO): DTO {
        val entity = findEntityById(id)
        validateBeforeUpdate(id, dto, entity)
        updateEntity(entity, dto)
        entity.updatedAt = Instant.now(clock)
        repository.save(entity)
        return toDto(entity)
    }

    @Transactional
    override fun delete(id: ID) {
        val entity = findEntityById(id)
        if (supportsSoftDelete()) {
            performSoftDelete(entity)
            repository.save(entity)
        } else {
            repository.delete(entity)
        }
    }

    protected fun findEntityById(id: ID): ENTITY {
        return if (supportsSoftDelete()) {
            @Suppress("UNCHECKED_CAST")
            val softDeleteMethods = repository as SoftDeleteRepositoryMethods<ENTITY, ID>
            @Suppress("UNCHECKED_CAST")
            softDeleteMethods.findByIdAndDeletedAtIsNull(id)
                .orElseThrow { notFoundException(id) } as ENTITY
        } else {
            repository.findById(id)
                .orElseThrow { notFoundException(id) }
        }
    }

    abstract fun toDto(entity: ENTITY): DTO
    protected abstract fun toEntity(dto: CREATE_DTO, createdAt: Instant): ENTITY
    protected abstract fun updateEntity(entity: ENTITY, dto: UPDATE_DTO)
    protected abstract fun validateBeforeCreate(dto: CREATE_DTO)
    protected abstract fun validateBeforeUpdate(id: ID, dto: UPDATE_DTO, entity: ENTITY)
    protected abstract fun notFoundException(id: ID): ResponseStatusException
    
    protected open fun supportsSoftDelete(): Boolean = false
    
    @Suppress("UNCHECKED_CAST")
    protected open fun performSoftDelete(entity: ENTITY) {
        val softDeletable = entity as? SoftDeletableEntity<ID>
        if (softDeletable != null) {
            val now = Instant.now(clock)
            softDeletable.deletedAt = now
            softDeletable.updatedAt = now
        }
    }
}

