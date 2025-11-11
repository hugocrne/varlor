package com.varlor.backend.product.security

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val issuer: String = "Varlor",
    val audience: String = "VarlorClients",
    val privateKeyPath: String = "classpath:keys/private_key.pem",
    val publicKeyPath: String = "classpath:keys/public_key.pem",
    val accessToken: TokenProperties = TokenProperties(Duration.ofMinutes(15)),
    val refreshToken: TokenProperties = TokenProperties(Duration.ofDays(7))
) {
    data class TokenProperties(
        val expiration: Duration
    )
}

