package com.varlor.backend.product.controller

import com.varlor.backend.product.model.dto.CreateUserSessionDto
import com.varlor.backend.product.model.dto.UpdateUserSessionDto
import com.varlor.backend.product.model.dto.UserSessionDto
import com.varlor.backend.product.service.UserSessionService
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
@RequestMapping("/api/user-sessions")
@Tag(name = "Sessions utilisateur", description = "Gestion des sessions utilisateur et refresh tokens")
@SecurityRequirement(name = "bearerAuth")
class UserSessionController(
    private val userSessionService: UserSessionService
) {

    @GetMapping
    @Operation(
        summary = "Lister les sessions utilisateur",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des sessions",
                content = [Content(array = ArraySchema(schema = Schema(implementation = UserSessionDto::class)))]
            )
        ]
    )
    fun findAll(): ResponseEntity<List<UserSessionDto>> =
        ResponseEntity.ok(userSessionService.findAll())

    @GetMapping("/{id}")
    @Operation(
        summary = "Récupérer une session",
        responses = [
            ApiResponse(responseCode = "200", description = "Session trouvée", content = [Content(schema = Schema(implementation = UserSessionDto::class))]),
            ApiResponse(responseCode = "404", description = "Session introuvable")
        ]
    )
    fun findById(@PathVariable id: UUID): ResponseEntity<UserSessionDto> =
        ResponseEntity.ok(userSessionService.findById(id))

    @PostMapping
    @Operation(
        summary = "Créer une session",
        responses = [
            ApiResponse(responseCode = "201", description = "Session créée", content = [Content(schema = Schema(implementation = UserSessionDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "409", description = "Conflit de session existante")
        ]
    )
    fun create(@Valid @RequestBody dto: CreateUserSessionDto): ResponseEntity<UserSessionDto> {
        val session = userSessionService.create(dto)
        return ResponseEntity.created(URI.create("/api/user-sessions/${session.id}")).body(session)
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Mettre à jour une session",
        responses = [
            ApiResponse(responseCode = "200", description = "Session mise à jour", content = [Content(schema = Schema(implementation = UserSessionDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "404", description = "Session introuvable"),
            ApiResponse(responseCode = "409", description = "Conflit de session existante")
        ]
    )
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateUserSessionDto
    ): ResponseEntity<UserSessionDto> =
        ResponseEntity.ok(userSessionService.update(id, dto))

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer une session",
        responses = [
            ApiResponse(responseCode = "204", description = "Suppression effectuée"),
            ApiResponse(responseCode = "404", description = "Session introuvable")
        ]
    )
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        userSessionService.delete(id)
        return ResponseEntity.noContent().build()
    }
}

