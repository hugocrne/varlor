package com.varlor.backend.product.controller

import com.varlor.backend.product.model.dto.ClientDto
import com.varlor.backend.product.model.dto.CreateClientDto
import com.varlor.backend.product.model.dto.UpdateClientDto
import com.varlor.backend.product.service.ClientService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.net.URI
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Gestion des clients")
@SecurityRequirement(name = "bearerAuth")
class ClientController(
    private val clientService: ClientService
) {

    @GetMapping
    @Operation(
        summary = "Lister les clients",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des clients",
                content = [Content(array = ArraySchema(schema = Schema(implementation = ClientDto::class)))]
            )
        ]
    )
    fun findAll(): ResponseEntity<List<ClientDto>> = ResponseEntity.ok(clientService.findAll())

    @GetMapping("/{id}")
    @Operation(
        summary = "Récupérer un client par identifiant",
        responses = [
            ApiResponse(responseCode = "200", description = "Client trouvé", content = [Content(schema = Schema(implementation = ClientDto::class))]),
            ApiResponse(responseCode = "404", description = "Client introuvable")
        ]
    )
    fun findById(@PathVariable id: UUID): ResponseEntity<ClientDto> =
        ResponseEntity.ok(clientService.findById(id))

    @PostMapping
    @Operation(
        summary = "Créer un client",
        responses = [
            ApiResponse(responseCode = "201", description = "Client créé", content = [Content(schema = Schema(implementation = ClientDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide")
        ]
    )
    fun create(@Valid @RequestBody dto: CreateClientDto): ResponseEntity<ClientDto> {
        val client = clientService.create(dto)
        return ResponseEntity.created(URI.create("/api/clients/${client.id}")).body(client)
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Mettre à jour un client",
        responses = [
            ApiResponse(responseCode = "200", description = "Client mis à jour", content = [Content(schema = Schema(implementation = ClientDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "404", description = "Client introuvable")
        ]
    )
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateClientDto
    ): ResponseEntity<ClientDto> = ResponseEntity.ok(clientService.update(id, dto))

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer (soft delete) un client",
        responses = [
            ApiResponse(responseCode = "204", description = "Suppression effectuée"),
            ApiResponse(responseCode = "404", description = "Client introuvable")
        ]
    )
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        clientService.delete(id)
        return ResponseEntity.noContent().build()
    }
}

