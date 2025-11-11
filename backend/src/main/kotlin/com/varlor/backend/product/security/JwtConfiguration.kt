package com.varlor.backend.product.security

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder

@Configuration
class JwtConfiguration(
    private val properties: JwtProperties,
    private val resourceLoader: ResourceLoader
) {

    @Bean
    fun rsaKey(): RSAKey {
        val publicKey = loadPublicKey(properties.publicKeyPath)
        val privateKey = loadPrivateKey(properties.privateKeyPath)
        return RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID("varlor-product-api-key")
            .build()
    }

    @Bean
    fun jwtDecoder(rsaKey: RSAKey): JwtDecoder {
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build()
    }

    @Bean
    fun jwtEncoder(rsaKey: RSAKey): JwtEncoder {
        val jwkSet = JWKSet(rsaKey)
        val jwkSource = ImmutableJWKSet<com.nimbusds.jose.proc.SecurityContext>(jwkSet)
        return NimbusJwtEncoder(jwkSource)
    }

    private fun loadPublicKey(path: String): RSAPublicKey {
        val pem = readPem(path)
        val content = pem.removeHeaderAndFooter("-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----")
        val keySpec = X509EncodedKeySpec(Base64.getMimeDecoder().decode(content))
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec) as RSAPublicKey
    }

    private fun loadPrivateKey(path: String): RSAPrivateKey {
        val pem = readPem(path)
        val normalized = when {
            pem.contains("-----BEGIN PRIVATE KEY-----") -> pem.removeHeaderAndFooter(
                "-----BEGIN PRIVATE KEY-----",
                "-----END PRIVATE KEY-----"
            )
            pem.contains("-----BEGIN RSA PRIVATE KEY-----") -> convertPkcs1ToPkcs8(
                pem.removeHeaderAndFooter(
                    "-----BEGIN RSA PRIVATE KEY-----",
                    "-----END RSA PRIVATE KEY-----"
                )
            )
            else -> throw IllegalArgumentException("Unsupported private key format for path $path")
        }

        val decoded = Base64.getMimeDecoder().decode(normalized)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(decoded)
        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun readPem(path: String): String {
        val resource = resourceLoader.getResource(path)
        if (!resource.exists()) {
            throw IllegalStateException("Unable to load key resource at $path")
        }
        resource.inputStream.use { input ->
            return BufferedReader(InputStreamReader(input)).readLines().joinToString("\n")
        }
    }

    private fun String.removeHeaderAndFooter(header: String, footer: String): String {
        return this
            .replace(header, "")
            .replace(footer, "")
            .replace("\\s".toRegex(), "")
    }

    private fun convertPkcs1ToPkcs8(content: String): String {
        val pkcs1Bytes = Base64.getMimeDecoder().decode(content)
        val pkcs1Length = pkcs1Bytes.size
        val totalLength = pkcs1Length + 22
        val header = byteArrayOf(
            0x30,
            0x82.toByte(),
            (totalLength shr 8).toByte(),
            (totalLength and 0xff).toByte(),
            0x02,
            0x01,
            0x00,
            0x30,
            0x0d,
            0x06,
            0x09,
            0x2a,
            0x86.toByte(),
            0x48,
            0x86.toByte(),
            0xf7.toByte(),
            0x0d,
            0x01,
            0x01,
            0x01,
            0x05,
            0x00,
            0x04,
            0x82.toByte(),
            (pkcs1Length shr 8).toByte(),
            (pkcs1Length and 0xff).toByte()
        )
        val pkcs8Bytes = header + pkcs1Bytes
        return Base64.getEncoder().encodeToString(pkcs8Bytes)
    }
}

