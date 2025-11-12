package com.varlor.backend.product.security

import com.varlor.backend.product.model.entity.User
import java.time.Instant
import java.util.UUID
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

@Component
class JwtProvider(
    private val properties: JwtProperties,
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: JwtDecoder
) {

    fun generateAccessToken(user: User, issuedAt: Instant = Instant.now()): GeneratedToken {
        val subject = user.id ?: throw IllegalStateException("User must have an identifier to issue a token.")
        val clientId = user.clientId ?: throw IllegalStateException("User must be linked to a client.")

        val expiresAt = issuedAt.plus(properties.accessToken.expiration)

        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(subject.toString())
            .audience(listOf(properties.audience))
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim("client_id", clientId.toString())
            .claim("role", user.role.name)
            // Email retiré pour réduire les données sensibles dans le token
            // L'email peut être récupéré depuis la base de données via le subject (user.id)
            .build()

        val token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue
        return GeneratedToken(token, issuedAt, expiresAt)
    }

    fun validate(token: String): JwtValidationResult {
        return try {
            val jwt = jwtDecoder.decode(token)
            JwtValidationResult(valid = true, jwt = jwt)
        } catch (exception: JwtException) {
            JwtValidationResult(valid = false, exception = exception)
        }
    }

    fun decode(token: String): Jwt = jwtDecoder.decode(token)

    data class GeneratedToken(
        val token: String,
        val issuedAt: Instant,
        val expiresAt: Instant
    )

    data class JwtValidationResult(
        val valid: Boolean,
        val jwt: Jwt? = null,
        val exception: Exception? = null
    ) {
        val subject: UUID?
            get() = jwt?.subject?.let(UUID::fromString)
    }
}

