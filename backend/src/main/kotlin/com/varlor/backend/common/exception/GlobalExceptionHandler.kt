package com.varlor.backend.common.exception

import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import jakarta.validation.ConstraintViolationException

/**
 * Gestionnaire global des exceptions pour l'API.
 *
 * Intercepte toutes les exceptions non gérées et retourne des réponses d'erreur standardisées
 * au format JSON avec la structure [ErrorResponse].
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "Invalide") 
        }
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "ValidationFailed",
            message = "La requête est invalide.",
            request = request,
            details = details
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val details = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to (violation.message ?: "Invalide")
        }
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "ConstraintViolation",
            message = "La requête est invalide.",
            request = request,
            details = details
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(
        ex: ResponseStatusException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val status = ex.statusCode
        val reason = ex.reason ?: status.toString()
        return buildResponse(
            status = status,
            error = reason,
            message = reason,
            request = request
        )
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleClientErrors(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return buildResponse(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            error = ex.javaClass.simpleName,
            message = ex.message ?: "La requête ne peut pas être traitée.",
            request = request
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleServerErrors(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Erreur interne lors du traitement d'une requête.", ex)
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            error = "InternalServerError",
            message = "Une erreur interne est survenue.",
            request = request
        )
    }

    private fun buildResponse(
        status: HttpStatusCode,
        error: String,
        message: String,
        request: WebRequest,
        details: Map<String, Any?>? = null
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = status.value(),
            error = error,
            message = message,
            path = request.getDescription(false).removePrefix("uri="),
            details = details
        )
        return ResponseEntity.status(status).body(response)
    }

    /**
     * Structure standardisée des réponses d'erreur de l'API.
     *
     * Toutes les erreurs retournées par l'API suivent ce format pour garantir
     * une expérience cohérente pour les clients.
     *
     * @property timestamp Date et heure de l'erreur (ISO 8601)
     * @property status Code HTTP de l'erreur (400, 401, 404, 422, 500, etc.)
     * @property error Type d'erreur (ValidationFailed, Not Found, etc.)
     * @property message Message d'erreur lisible par l'utilisateur
     * @property path Chemin de la requête qui a causé l'erreur
     * @property details Détails supplémentaires (ex: erreurs de validation par champ)
     */
    @Schema(
        description = "Réponse d'erreur standardisée de l'API",
        example = """{
  "timestamp": "2025-01-27T10:00:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "La requête est invalide.",
  "path": "/api/users",
  "details": {
    "email": "L'email doit être valide",
    "password": "Le mot de passe doit contenir au moins 8 caractères"
  }
}"""
    )
    data class ErrorResponse(
        @Schema(description = "Date et heure de l'erreur au format ISO 8601", example = "2025-01-27T10:00:00Z")
        val timestamp: Instant = Instant.now(),
        
        @Schema(description = "Code HTTP de l'erreur", example = "400")
        val status: Int,
        
        @Schema(description = "Type d'erreur", example = "ValidationFailed")
        val error: String,
        
        @Schema(description = "Message d'erreur lisible par l'utilisateur", example = "La requête est invalide.")
        val message: String,
        
        @Schema(description = "Chemin de la requête qui a causé l'erreur", example = "/api/users", nullable = true)
        val path: String?,
        
        @Schema(
            description = "Détails supplémentaires de l'erreur (ex: erreurs de validation par champ)",
            example = """{"email": "L'email doit être valide"}""",
            nullable = true
        )
        val details: Map<String, Any?>? = null
    )
}

