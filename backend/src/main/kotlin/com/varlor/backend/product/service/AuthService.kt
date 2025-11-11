package com.varlor.backend.product.service

import com.varlor.backend.product.model.dto.LoginRequestDto
import com.varlor.backend.product.model.dto.LoginResponseDto
import com.varlor.backend.product.model.dto.LogoutRequestDto
import com.varlor.backend.product.model.dto.RefreshTokenRequestDto
import com.varlor.backend.product.model.dto.RegisterRequestDto
import com.varlor.backend.product.model.dto.TokenPairResponseDto
import com.varlor.backend.product.model.dto.UserDto
import com.varlor.backend.product.model.dto.ValidateTokenResponseDto
import com.varlor.backend.product.model.entity.User
import com.varlor.backend.product.model.entity.UserSession
import com.varlor.backend.product.model.entity.UserStatus
import com.varlor.backend.product.repository.ClientRepository
import com.varlor.backend.product.repository.UserRepository
import com.varlor.backend.product.repository.UserSessionRepository
import com.varlor.backend.product.security.JwtProperties
import com.varlor.backend.product.security.JwtProvider
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Locale
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val clientRepository: ClientRepository,
    private val userSessionRepository: UserSessionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    private val clock: Clock = Clock.systemUTC()
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val secureRandom = SecureRandom()

    @Transactional
    fun register(request: RegisterRequestDto): UserDto {
        val clientExists = clientRepository.existsByIdAndDeletedAtIsNull(request.clientId)
        if (!clientExists) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Client associé introuvable ou supprimé.")
        }

        val existingUser = userRepository.findByEmailAndDeletedAtIsNull(request.email)
        if (existingUser != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur avec cet email existe déjà.")
        }

        val now = Instant.now(clock)
        val user = User(
            clientId = request.clientId,
            email = request.email.lowercase(Locale.getDefault()),
            passwordHash = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            role = request.role,
            status = request.status
        ).apply {
            createdAt = now
            updatedAt = now
        }

        val saved = userRepository.save(user)
        return mapToUserDto(saved)
    }

    @Transactional
    fun login(request: LoginRequestDto, ipAddress: String, userAgent: String): TokenPairResponseDto {
        val now = Instant.now(clock)
        val user = userRepository.findByEmailAndDeletedAtIsNull(request.email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable.")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides.")
        }

        if (user.status != UserStatus.ACTIVE || user.deletedAt != null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Le compte utilisateur est inactif.")
        }

        cleanupExpiredSessions(user.id!!, now)

        val tokenPair = issueTokenPair(user, ipAddress, userAgent, now, null)

        user.lastLoginAt = now
        user.updatedAt = now

        return LoginResponseDto(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            expiresAt = tokenPair.expiresAt,
            refreshExpiresAt = tokenPair.refreshExpiresAt
        )
    }

    @Transactional
    fun refreshToken(request: RefreshTokenRequestDto, ipAddress: String, userAgent: String): TokenPairResponseDto {
        val now = Instant.now(clock)
        val hashed = hashRefreshToken(request.refreshToken)

        val session = userSessionRepository.findByTokenHash(hashed)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide.") }

        if (session.revokedAt != null || session.expiresAt <= now) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expiré ou révoqué.")
        }

        val user = session.user ?: userRepository.findById(session.userId!!)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur invalide.") }

        if (user.deletedAt != null || user.status != UserStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur invalide.")
        }

        cleanupExpiredSessions(user.id!!, now)

        val tokenPair = issueTokenPair(user, ipAddress, userAgent, now, session)

        return tokenPair
    }

    @Transactional
    fun logout(request: LogoutRequestDto) {
        val now = Instant.now(clock)
        val hashed = hashRefreshToken(request.refreshToken)

        val sessionOptional = userSessionRepository.findByTokenHash(hashed)
        if (sessionOptional.isEmpty) {
            return
        }

        val session = sessionOptional.get()

        if (request.revokeAllSessions) {
            val sessions = userSessionRepository.findAllByUserIdAndRevokedAtIsNull(session.userId!!)
            sessions.forEach { userSession ->
                if (userSession.revokedAt == null) {
                    userSession.revokedAt = now
                    userSession.revocationReason = REVOCATION_REASON_LOGOUT
                    userSession.replacedByTokenId = null
                }
            }
        } else if (session.revokedAt == null) {
            session.revokedAt = now
            session.revocationReason = REVOCATION_REASON_LOGOUT
            session.replacedByTokenId = null
        }
    }

    fun validateToken(token: String): ValidateTokenResponseDto {
        val validation = jwtProvider.validate(token)
        return if (validation.valid && validation.jwt != null) {
            ValidateTokenResponseDto(
                valid = true,
                subject = validation.jwt.subject,
                expiresAt = validation.jwt.expiresAt
            )
        } else {
            ValidateTokenResponseDto(valid = false)
        }
    }

    private fun issueTokenPair(
        user: User,
        ipAddress: String,
        userAgent: String,
        now: Instant,
        sessionToRevoke: UserSession?
    ): TokenPairResponseDto {
        val accessToken = jwtProvider.generateAccessToken(user, now)
        val refreshToken = generateRefreshToken()
        val refreshTokenHash = hashRefreshToken(refreshToken)
        val refreshTokenExpiresAt = now.plus(jwtProperties.refreshToken.expiration)
        val tokenId = UUID.randomUUID().toString().replace("-", "")

        val session = UserSession(
            userId = user.id,
            tokenId = tokenId,
            tokenHash = refreshTokenHash,
            ipAddress = sanitizeIpAddress(ipAddress),
            userAgent = sanitizeUserAgent(userAgent),
            expiresAt = refreshTokenExpiresAt
        ).apply {
            createdAt = now
            revokedAt = null
            replacedByTokenId = null
            revocationReason = null
        }

        sessionToRevoke?.let {
            it.revokedAt = now
            it.revocationReason = REVOCATION_REASON_ROTATION
            it.replacedByTokenId = tokenId
        }

        userSessionRepository.save(session)

        return TokenPairResponseDto(
            accessToken = accessToken.token,
            refreshToken = refreshToken,
            expiresAt = accessToken.expiresAt,
            refreshExpiresAt = refreshTokenExpiresAt
        )
    }

    private fun cleanupExpiredSessions(userId: UUID, now: Instant) {
        val removed = userSessionRepository.deleteAllByUserIdAndExpiresAtLessThanEqual(userId, now)
        if (removed > 0) {
            logger.debug("Removed {} expired sessions for user {}", removed, userId)
        }
    }

    private fun generateRefreshToken(): String {
        val buffer = ByteArray(REFRESH_TOKEN_BYTE_LENGTH)
        secureRandom.nextBytes(buffer)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }

    private fun hashRefreshToken(refreshToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(refreshToken.toByteArray(StandardCharsets.UTF_8))
        return java.util.Base64.getEncoder().encodeToString(hash)
    }

    private fun sanitizeIpAddress(ipAddress: String): String {
        if (ipAddress.isBlank()) {
            return "unknown"
        }
        return if (ipAddress.length > MAX_IP_LENGTH) {
            ipAddress.substring(0, MAX_IP_LENGTH)
        } else {
            ipAddress
        }
    }

    private fun sanitizeUserAgent(userAgent: String): String {
        val value = if (userAgent.isBlank()) "unknown" else userAgent.trim()
        return if (value.length > MAX_USER_AGENT_LENGTH) {
            value.substring(0, MAX_USER_AGENT_LENGTH)
        } else {
            value
        }
    }

    private fun mapToUserDto(user: User): UserDto {
        return UserDto(
            id = user.id!!,
            clientId = user.clientId!!,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role,
            status = user.status,
            lastLoginAt = user.lastLoginAt,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            deletedAt = user.deletedAt
        )
    }

    companion object {
        private const val REFRESH_TOKEN_BYTE_LENGTH = 64
        private const val MAX_IP_LENGTH = 45
        private const val MAX_USER_AGENT_LENGTH = 500
        private const val REVOCATION_REASON_LOGOUT = "LOGOUT"
        private const val REVOCATION_REASON_ROTATION = "ROTATED"
    }
}

