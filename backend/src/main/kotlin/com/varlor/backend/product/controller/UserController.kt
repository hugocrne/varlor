package com.varlor.backend.product.controller

import com.varlor.backend.product.model.dto.CreateUserDto
import com.varlor.backend.product.model.dto.UpdateUserDto
import com.varlor.backend.product.model.dto.UserDto
import com.varlor.backend.product.service.UserService
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
@RequestMapping("/api/users")
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    @Operation(
        summary = "Lister les utilisateurs",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des utilisateurs",
                content = [Content(array = ArraySchema(schema = Schema(implementation = UserDto::class)))]
            )
        ]
    )
    fun findAll(): ResponseEntity<List<UserDto>> = ResponseEntity.ok(userService.findAll())

    @GetMapping("/{id}")
    @Operation(
        summary = "Récupérer un utilisateur par identifiant",
        responses = [
            ApiResponse(responseCode = "200", description = "Utilisateur trouvé", content = [Content(schema = Schema(implementation = UserDto::class))]),
            ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
        ]
    )
    fun findById(@PathVariable id: UUID): ResponseEntity<UserDto> =
        ResponseEntity.ok(userService.findById(id))

    @PostMapping
    @Operation(
        summary = "Créer un utilisateur",
        responses = [
            ApiResponse(responseCode = "201", description = "Utilisateur créé", content = [Content(schema = Schema(implementation = UserDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "409", description = "Utilisateur déjà existant")
        ]
    )
    fun create(@Valid @RequestBody dto: CreateUserDto): ResponseEntity<UserDto> {
        val user = userService.create(dto)
        return ResponseEntity.created(URI.create("/api/users/${user.id}")).body(user)
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Mettre à jour un utilisateur",
        responses = [
            ApiResponse(responseCode = "200", description = "Utilisateur mis à jour", content = [Content(schema = Schema(implementation = UserDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
            ApiResponse(responseCode = "409", description = "Conflit de données")
        ]
    )
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateUserDto
    ): ResponseEntity<UserDto> =
        ResponseEntity.ok(userService.update(id, dto))

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer (soft delete) un utilisateur",
        responses = [
            ApiResponse(responseCode = "204", description = "Suppression effectuée"),
            ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
        ]
    )
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        userService.delete(id)
        return ResponseEntity.noContent().build()
    }
}

