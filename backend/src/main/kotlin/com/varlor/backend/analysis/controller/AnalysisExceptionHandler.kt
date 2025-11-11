package com.varlor.backend.analysis.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@RestControllerAdvice(basePackageClasses = [AnalysisController::class])
class AnalysisExceptionHandler {

    private val logger = LoggerFactory.getLogger(AnalysisExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalide") }
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "ValidationFailed",
            message = "La requête est invalide.",
            request = request,
            details = details
        )
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        IllegalStateException::class
    )
    fun handleClientErrors(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            error = ex.javaClass.simpleName,
            message = ex.message ?: "La requête ne peut pas être traitée.",
            request = request
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleServerErrors(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Erreur interne lors du traitement de la requête d'analyse.", ex)
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            error = "InternalServerError",
            message = "Une erreur interne est survenue.",
            request = request
        )
    }

    private fun buildResponse(
        status: HttpStatus,
        error: String,
        message: String,
        request: WebRequest,
        details: Map<String, Any>? = null
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

    data class ErrorResponse(
        val timestamp: Instant = Instant.now(),
        val status: Int,
        val error: String,
        val message: String,
        val path: String?,
        val details: Map<String, Any>? = null
    )
}

