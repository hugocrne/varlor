package com.varlor.backend.product.controller

import com.varlor.backend.product.model.dto.CreateUserPreferenceDto
import com.varlor.backend.product.model.dto.UpdateUserPreferenceDto
import com.varlor.backend.product.model.dto.UserPreferenceDto
import com.varlor.backend.product.service.UserPreferenceService
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
@RequestMapping("/api/user-preferences")
@Tag(name = "Préférences utilisateur", description = "Gestion des préférences utilisateur")
@SecurityRequirement(name = "bearerAuth")
class UserPreferenceController(
    private val userPreferenceService: UserPreferenceService
) {

    @GetMapping
    @Operation(
        summary = "Lister les préférences utilisateur",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des préférences",
                content = [Content(array = ArraySchema(schema = Schema(implementation = UserPreferenceDto::class)))]
            )
        ]
    )
    fun findAll(): ResponseEntity<List<UserPreferenceDto>> =
        ResponseEntity.ok(userPreferenceService.findAll())

    @GetMapping("/{id}")
    @Operation(
        summary = "Récupérer une préférence utilisateur",
        responses = [
            ApiResponse(responseCode = "200", description = "Préférence trouvée", content = [Content(schema = Schema(implementation = UserPreferenceDto::class))]),
            ApiResponse(responseCode = "404", description = "Préférence introuvable")
        ]
    )
    fun findById(@PathVariable id: UUID): ResponseEntity<UserPreferenceDto> =
        ResponseEntity.ok(userPreferenceService.findById(id))

    @PostMapping
    @Operation(
        summary = "Créer une préférence utilisateur",
        responses = [
            ApiResponse(responseCode = "201", description = "Préférence créée", content = [Content(schema = Schema(implementation = UserPreferenceDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "409", description = "Préférence déjà existante pour l'utilisateur")
        ]
    )
    fun create(@Valid @RequestBody dto: CreateUserPreferenceDto): ResponseEntity<UserPreferenceDto> {
        val preference = userPreferenceService.create(dto)
        return ResponseEntity.created(URI.create("/api/user-preferences/${preference.id}")).body(preference)
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Mettre à jour une préférence utilisateur",
        responses = [
            ApiResponse(responseCode = "200", description = "Préférence mise à jour", content = [Content(schema = Schema(implementation = UserPreferenceDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "404", description = "Préférence introuvable")
        ]
    )
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateUserPreferenceDto
    ): ResponseEntity<UserPreferenceDto> =
        ResponseEntity.ok(userPreferenceService.update(id, dto))

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer une préférence utilisateur",
        responses = [
            ApiResponse(responseCode = "204", description = "Suppression effectuée"),
            ApiResponse(responseCode = "404", description = "Préférence introuvable")
        ]
    )
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        userPreferenceService.delete(id)
        return ResponseEntity.noContent().build()
    }
}

