package com.varlor.backend.product.service

import com.varlor.backend.common.service.BaseCrudService
import com.varlor.backend.product.model.dto.ClientDto
import com.varlor.backend.product.model.dto.CreateClientDto
import com.varlor.backend.product.model.dto.UpdateClientDto
import com.varlor.backend.product.model.entity.Client
import com.varlor.backend.product.model.entity.ClientStatus
import com.varlor.backend.product.repository.ClientRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ClientService(
    clientRepository: ClientRepository,
    clock: Clock = Clock.systemUTC()
) : BaseCrudService<Client, ClientDto, CreateClientDto, UpdateClientDto, UUID>(
    repository = clientRepository,
    clock = clock
) {

    override fun supportsSoftDelete(): Boolean = true

    override fun toDto(entity: Client): ClientDto {
        return ClientDto(
            id = entity.id!!,
            name = entity.name,
            type = entity.type,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt
        )
    }

    override fun toEntity(dto: CreateClientDto, createdAt: Instant): Client {
        return Client(
            name = dto.name,
            type = dto.type,
            status = dto.status
        ).apply {
            this.createdAt = createdAt
            this.updatedAt = createdAt
            this.deletedAt = null
        }
    }

    override fun updateEntity(entity: Client, dto: UpdateClientDto) {
        dto.name?.let { entity.name = it }
        dto.type?.let { entity.type = it }
        dto.status?.let { entity.status = it }
    }

    override fun validateBeforeCreate(dto: CreateClientDto) {
        // Aucune validation spécifique nécessaire
    }

    override fun validateBeforeUpdate(id: UUID, dto: UpdateClientDto, entity: Client) {
        if (dto.name == null && dto.type == null && dto.status == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune donnée de mise à jour fournie.")
        }
    }

    override fun notFoundException(id: UUID): ResponseStatusException =
        ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable.")

    override fun performSoftDelete(entity: Client) {
        super.performSoftDelete(entity)
        entity.status = ClientStatus.INACTIVE
    }
}

