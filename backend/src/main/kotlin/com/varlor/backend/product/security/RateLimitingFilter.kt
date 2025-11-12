package com.varlor.backend.product.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Filtre de rate limiting simple pour protéger les endpoints d'authentification
 * contre les attaques par force brute.
 * 
 * Utilise un cache en mémoire avec expiration automatique.
 * Pour une solution de production, considérer l'utilisation de Redis ou Bucket4j.
 */
@Component
@Order(1)
class RateLimitingFilter(
    @Value("\${app.rate-limiting.enabled:true}")
    private val enabled: Boolean,
    @Value("\${app.rate-limiting.max-requests:5}")
    private val maxRequests: Int,
    @Value("\${app.rate-limiting.window-seconds:60}")
    private val windowSeconds: Long
) : OncePerRequestFilter() {

    private class RequestInfo(
        val count: AtomicInteger = AtomicInteger(0),
        @Volatile var windowStart: Long = System.currentTimeMillis()
    )

    private val requestCounts = ConcurrentHashMap<String, RequestInfo>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!enabled) {
            filterChain.doFilter(request, response)
            return
        }

        // Appliquer le rate limiting uniquement sur les endpoints d'authentification
        val path = request.requestURI
        if (!path.startsWith("/api/auth/login") && !path.startsWith("/api/auth/register")) {
            filterChain.doFilter(request, response)
            return
        }

        val clientKey = getClientKey(request)
        val now = System.currentTimeMillis()

        val info = requestCounts.computeIfAbsent(clientKey) {
            RequestInfo(windowStart = now)
        }

        // Réinitialiser le compteur si la fenêtre de temps est expirée
        synchronized(info) {
            val elapsed = now - info.windowStart
            if (elapsed > windowSeconds * 1000) {
                info.count.set(0)
                info.windowStart = now
            }

            val currentCount = info.count.incrementAndGet()
            if (currentCount > maxRequests) {
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.contentType = "application/json"
                response.writer.write("""{"error":"Trop de requêtes. Veuillez réessayer plus tard."}""")
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun getClientKey(request: HttpServletRequest): String {
        // Utiliser l'IP comme clé, ou l'email si disponible dans la requête
        val ip = request.remoteAddr ?: "unknown"
        val email = request.getParameter("email")
        return if (email != null) {
            "$ip:$email"
        } else {
            ip
        }
    }
}

