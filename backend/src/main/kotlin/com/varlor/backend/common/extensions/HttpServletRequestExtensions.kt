package com.varlor.backend.common.extensions

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Informations client extraites depuis la requête HTTP.
 * 
 * @param ipAddress l'adresse IP du client (ou "unknown" si non disponible)
 * @param userAgent le User-Agent du client (ou "unknown" si non disponible)
 */
data class ClientInfo(
    val ipAddress: String,
    val userAgent: String
)

/**
 * Extrait l'adresse IP et le User-Agent depuis la requête HTTP.
 * 
 * Cette extension centralise l'extraction des informations client depuis
 * HttpServletRequest, en fournissant des valeurs par défaut si les informations
 * ne sont pas disponibles.
 * 
 * @return une instance de [ClientInfo] contenant l'IP et le User-Agent
 * 
 * @example
 * ```
 * val clientInfo = httpRequest.extractClientInfo()
 * val response = authService.login(request, clientInfo.ipAddress, clientInfo.userAgent)
 * ```
 */
fun HttpServletRequest.extractClientInfo(): ClientInfo {
    return ClientInfo(
        ipAddress = this.remoteAddr ?: "unknown",
        userAgent = this.getHeader("User-Agent") ?: "unknown"
    )
}

/**
 * Extrait le token Bearer depuis l'en-tête Authorization.
 * 
 * Cette extension valide et extrait le token Bearer depuis l'en-tête Authorization
 * de la requête HTTP, en lançant une exception appropriée si le token est manquant
 * ou invalide.
 * 
 * @return le token Bearer extrait et validé
 * @throws ResponseStatusException si le token est manquant ou invalide
 * 
 * @example
 * ```
 * val token = request.extractBearerToken()
 * val response = authService.validateToken(token)
 * ```
 */
fun HttpServletRequest.extractBearerToken(): String {
    val authorization = this.getHeader("Authorization")
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "En-tête Authorization manquant.")
    
    if (!authorization.startsWith("Bearer ")) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Le jeton doit être fourni avec le préfixe Bearer.")
    }
    
    val token = authorization.removePrefix("Bearer ").trim()
    if (token.isEmpty()) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Le jeton ne peut pas être vide.")
    }
    
    return token
}

