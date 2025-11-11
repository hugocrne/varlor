package com.varlor.backend.product.service

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
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ClientService(
    private val clientRepository: ClientRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun findAll(): List<ClientDto> {
        return clientRepository.findAllByDeletedAtIsNull().map(::toDto)
    }

    fun findById(id: UUID): ClientDto {
        val client = clientRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable.") }
        return toDto(client)
    }

    @Transactional
    fun create(dto: CreateClientDto): ClientDto {
        val now = Instant.now(clock)
        val client = Client(
            name = dto.name,
            type = dto.type,
            status = dto.status
        ).apply {
            createdAt = now
            updatedAt = now
            deletedAt = null
        }

        val saved = clientRepository.save(client)
        return toDto(saved)
    }

    @Transactional
    fun update(id: UUID, dto: UpdateClientDto): ClientDto {
        if (dto.name == null && dto.type == null && dto.status == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune donnée de mise à jour fournie.")
        }

        val client = clientRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable.") }

        dto.name?.let { client.name = it }
        dto.type?.let { client.type = it }
        dto.status?.let { client.status = it }

        client.updatedAt = Instant.now(clock)

        return toDto(client)
    }

    @Transactional
    fun delete(id: UUID) {
        val client = clientRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable.") }

        val now = Instant.now(clock)
        client.status = ClientStatus.INACTIVE
        client.deletedAt = now
        client.updatedAt = now
    }

    private fun toDto(client: Client): ClientDto {
        return ClientDto(
            id = client.id!!,
            name = client.name,
            type = client.type,
            status = client.status,
            createdAt = client.createdAt,
            updatedAt = client.updatedAt,
            deletedAt = client.deletedAt
        )
    }
}

