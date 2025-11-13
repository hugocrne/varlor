package com.varlor.backend.common.controller

import com.varlor.backend.common.exception.GlobalExceptionHandler
import com.varlor.backend.common.model.IdentifiableDto
import com.varlor.backend.common.service.CrudService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
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

/**
 * Contrôleur de base pour les opérations CRUD standard.
 *
 * Fournit les endpoints REST standard pour créer, lire, mettre à jour et supprimer des entités.
 * Les classes filles doivent fournir les DTOs spécifiques et le service correspondant.
 *
 * @param DTO Le type de DTO de réponse
 * @param CREATE_DTO Le type de DTO pour la création
 * @param UPDATE_DTO Le type de DTO pour la mise à jour
 * @param ID Le type de l'identifiant (UUID, Long, etc.)
 */
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
        description = "Récupère la liste complète des entités.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des entités récupérée avec succès",
                content = [Content(array = ArraySchema(schema = Schema(implementation = Any::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Non authentifié",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Erreur serveur",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            )
        ]
    )
    fun findAll(): ResponseEntity<List<DTO>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/{id}")
    @Operation(
        summary = "Récupérer une entité par identifiant",
        description = "Récupère une entité spécifique à partir de son identifiant.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Entité trouvée",
                content = [Content(schema = Schema(implementation = Any::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Entité introuvable",
                content = [Content(
                    schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Non trouvé",
                            value = """{
  "timestamp": "2025-01-27T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Entité introuvable.",
  "path": "/api/entities/{id}"
}"""
                        )
                    ]
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Non authentifié",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Erreur serveur",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            )
        ]
    )
    fun findById(@PathVariable id: ID): ResponseEntity<DTO> =
        ResponseEntity.ok(service.findById(id))

    @PostMapping
    @Operation(
        summary = "Créer une entité",
        description = "Crée une nouvelle entité avec les données fournies.",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "Entité créée avec succès",
                content = [Content(schema = Schema(implementation = Any::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide",
                content = [Content(
                    schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Validation échouée",
                            value = """{
  "timestamp": "2025-01-27T10:00:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "La requête est invalide.",
  "path": "/api/entities",
  "details": {
    "field": "Le champ est requis"
  }
}"""
                        )
                    ]
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Non authentifié",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Erreur serveur",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            )
        ]
    )
    open fun create(@Valid @RequestBody dto: CREATE_DTO): ResponseEntity<DTO> {
        val entity = service.create(dto)
        return ResponseEntity.created(URI.create("$basePath/${entity.id}")).body(entity)
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Mettre à jour une entité",
        description = "Met à jour une entité existante avec les données fournies. Seuls les champs fournis seront mis à jour.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Entité mise à jour avec succès",
                content = [Content(schema = Schema(implementation = Any::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Entité introuvable",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Non authentifié",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Erreur serveur",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            )
        ]
    )
    open fun update(
        @PathVariable id: ID,
        @Valid @RequestBody dto: UPDATE_DTO
    ): ResponseEntity<DTO> = ResponseEntity.ok(service.update(id, dto))

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer une entité",
        description = "Supprime une entité existante. La suppression peut être physique ou logique selon le type d'entité.",
        responses = [
            ApiResponse(
                responseCode = "204",
                description = "Suppression effectuée avec succès"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Entité introuvable",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Non authentifié",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Erreur serveur",
                content = [Content(schema = Schema(implementation = GlobalExceptionHandler.ErrorResponse::class))]
            )
        ]
    )
    fun delete(@PathVariable id: ID): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}

