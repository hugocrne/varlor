package com.varlor.backend.common.controller

import com.varlor.backend.common.model.IdentifiableDto
import com.varlor.backend.common.service.CrudService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import java.io.Serializable
import java.net.URI
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

abstract class BaseCrudController<
    DTO : IdentifiableDto<ID>,
    CREATE_DTO : Any,
    UPDATE_DTO : Any,
    ID : Serializable
>(
    protected val service: CrudService<DTO, CREATE_DTO, UPDATE_DTO, ID>,
    protected val basePath: String,
    protected val entityName: String,
    protected val dtoClass: Class<DTO>
) {

    @GetMapping
    @Operation(
        summary = "Lister les entités",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des entités",
                content = [Content(array = ArraySchema(schema = Schema(implementation = Any::class)))]
            )
        ]
    )
    fun findAll(): ResponseEntity<List<DTO>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/{id}")
    @Operation(
        summary = "Récupérer une entité par identifiant",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Entité trouvée",
                content = [Content(schema = Schema(implementation = Any::class))]
            ),
            ApiResponse(responseCode = "404", description = "Entité introuvable")
        ]
    )
    fun findById(@PathVariable id: ID): ResponseEntity<DTO> =
        ResponseEntity.ok(service.findById(id))

    @PostMapping
    @Operation(
        summary = "Créer une entité",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "Entité créée",
                content = [Content(schema = Schema(implementation = Any::class))]
            ),
            ApiResponse(responseCode = "400", description = "Requête invalide")
        ]
    )
    open fun create(@Valid @RequestBody dto: CREATE_DTO): ResponseEntity<DTO> {
        val entity = service.create(dto)
        return ResponseEntity.created(URI.create("$basePath/${entity.id}")).body(entity)
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Mettre à jour une entité",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Entité mise à jour",
                content = [Content(schema = Schema(implementation = Any::class))]
            ),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "404", description = "Entité introuvable")
        ]
    )
    open fun update(
        @PathVariable id: ID,
        @Valid @RequestBody dto: UPDATE_DTO
    ): ResponseEntity<DTO> = ResponseEntity.ok(service.update(id, dto))

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer une entité",
        responses = [
            ApiResponse(responseCode = "204", description = "Suppression effectuée"),
            ApiResponse(responseCode = "404", description = "Entité introuvable")
        ]
    )
    fun delete(@PathVariable id: ID): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}

