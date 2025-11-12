package com.varlor.backend.product.controller

import com.varlor.backend.common.extensions.extractBearerToken
import com.varlor.backend.common.extensions.extractClientInfo
import com.varlor.backend.product.model.dto.LoginRequestDto
import com.varlor.backend.product.model.dto.LoginResponseDto
import com.varlor.backend.product.model.dto.LogoutRequestDto
import com.varlor.backend.product.model.dto.RefreshTokenRequestDto
import com.varlor.backend.product.model.dto.RegisterRequestDto
import com.varlor.backend.product.model.dto.TokenPairResponseDto
import com.varlor.backend.product.model.dto.UserDto
import com.varlor.backend.product.model.dto.ValidateTokenResponseDto
import com.varlor.backend.product.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints d'authentification JWT")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @Operation(
        summary = "Inscrire un nouvel utilisateur",
        description = "Crée un utilisateur et retourne ses informations.",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    schema = Schema(implementation = RegisterRequestDto::class),
                    examples = [
                        io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Inscription",
                            value = """{
  "clientId": "11111111-2222-3333-4444-555555555555",
  "email": "nouvel.utilisateur@varlor.io",
  "password": "Secret123!",
  "firstName": "Nouveau",
  "lastName": "Utilisateur",
  "role": "MEMBER",
  "status": "ACTIVE"
}"""
                        )
                    ]
                )
            ]
        ),
        responses = [
            ApiResponse(responseCode = "201", description = "Utilisateur créé", content = [Content(schema = Schema(implementation = UserDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "409", description = "Utilisateur déjà existant")
        ]
    )
    fun register(@Valid @RequestBody request: RegisterRequestDto): ResponseEntity<UserDto> {
        val user = authService.register(request)
        return ResponseEntity.created(URI.create("/api/users/${user.id}")).body(user)
    }

    @PostMapping("/login")
    @Operation(
        summary = "Connexion utilisateur",
        description = "Authentifie un utilisateur et retourne un couple de jetons JWT.",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    schema = Schema(implementation = LoginRequestDto::class),
                    examples = [
                        io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Connexion",
                            value = """{
  "email": "owner@varlor.io",
  "password": "Secret123!"
}"""
                        )
                    ]
                )
            ]
        ),
        responses = [
            ApiResponse(responseCode = "200", description = "Connexion réussie", content = [Content(schema = Schema(implementation = LoginResponseDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "401", description = "Identifiants incorrects"),
            ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
        ]
    )
    fun login(
        @Valid @RequestBody request: LoginRequestDto,
        httpRequest: HttpServletRequest
    ): ResponseEntity<LoginResponseDto> {
        val clientInfo = httpRequest.extractClientInfo()
        val response = authService.login(request, clientInfo.ipAddress, clientInfo.userAgent)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Renouveler un jeton",
        description = "Régénère un couple de jetons à partir d'un refresh token valide.",
        responses = [
            ApiResponse(responseCode = "200", description = "Jeton régénéré", content = [Content(schema = Schema(implementation = TokenPairResponseDto::class))]),
            ApiResponse(responseCode = "400", description = "Requête invalide"),
            ApiResponse(responseCode = "401", description = "Refresh token invalide ou expiré")
        ]
    )
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequestDto,
        httpRequest: HttpServletRequest
    ): ResponseEntity<TokenPairResponseDto> {
        val clientInfo = httpRequest.extractClientInfo()
        val response = authService.refreshToken(request, clientInfo.ipAddress, clientInfo.userAgent)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Déconnexion",
        description = "Révoque un refresh token et, optionnellement, toutes les sessions de l'utilisateur.",
        responses = [
            ApiResponse(responseCode = "204", description = "Déconnexion effectuée"),
            ApiResponse(responseCode = "400", description = "Requête invalide")
        ]
    )
    fun logout(@Valid @RequestBody request: LogoutRequestDto): ResponseEntity<Void> {
        authService.logout(request)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/validate")
    @Operation(
        summary = "Valider un jeton d'accès",
        description = "Vérifie la validité d'un jeton JWT fourni dans l'en-tête Authorization.",
        responses = [
            ApiResponse(responseCode = "200", description = "Jeton valide", content = [Content(schema = Schema(implementation = ValidateTokenResponseDto::class))]),
            ApiResponse(responseCode = "400", description = "Jeton manquant ou invalide")
        ]
    )
    fun validate(request: HttpServletRequest): ResponseEntity<ValidateTokenResponseDto> {
        val token = request.extractBearerToken()
        val response = authService.validateToken(token)
        return ResponseEntity.ok(response)
    }
}

