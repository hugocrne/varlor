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
import com.varlor.backend.common.extensions.normalizeEmail
import com.varlor.backend.common.extensions.requireExists
import com.varlor.backend.common.repository.SoftDeleteRepositoryMethods
import com.varlor.backend.common.util.ErrorMessages
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * Service d'authentification et de gestion des jetons JWT.
 *
 * Gère l'inscription des utilisateurs, la connexion, le renouvellement des jetons,
 * la déconnexion et la validation des jetons d'accès.
 *
 * Sécurité :
 * - Protection contre l'énumération d'utilisateurs (même message d'erreur)
 * - Protection contre les attaques de timing (toujours exécuter passwordEncoder.matches)
 * - Rotation des refresh tokens lors du renouvellement
 * - Nettoyage automatique des sessions expirées
 *
 * @property userRepository Repository des utilisateurs
 * @property clientRepository Repository des clients
 * @property userSessionRepository Repository des sessions utilisateur
 * @property passwordEncoder Encodeur de mots de passe (BCrypt)
 * @property jwtProvider Générateur et validateur de jetons JWT
 * @property jwtProperties Propriétés de configuration JWT
 * @property userService Service de gestion des utilisateurs
 * @property clock Horloge pour la gestion du temps (injectable pour les tests)
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val clientRepository: ClientRepository,
    private val userSessionRepository: UserSessionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    private val userService: UserService,
    private val clock: Clock = Clock.systemUTC()
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val secureRandom = SecureRandom()

    /**
     * Inscrit un nouvel utilisateur dans le système.
     *
     * Crée un utilisateur avec les informations fournies, encode le mot de passe
     * et retourne les informations de l'utilisateur créé.
     *
     * @param request DTO contenant les informations d'inscription
     * @return DTO de l'utilisateur créé
     * @throws ResponseStatusException avec code 409 si l'utilisateur existe déjà
     * @throws ResponseStatusException avec code 404 si le client n'existe pas
     */
    @Transactional
    fun register(request: RegisterRequestDto): UserDto {
        (clientRepository as SoftDeleteRepositoryMethods<*, UUID>)
            .requireExists(request.clientId, "Client")

        val normalizedEmail = request.email.normalizeEmail()
        val existingUser = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail)
        if (existingUser != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, ErrorMessages.CONFLICT)
        }

        val now = Instant.now(clock)
        val user = User(
            clientId = request.clientId,
            email = normalizedEmail,
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
        return userService.toDto(saved)
    }

    /**
     * Authentifie un utilisateur et génère un couple de jetons JWT.
     *
     * Vérifie les identifiants, l'état de l'utilisateur et génère un access token
     * et un refresh token. Enregistre également une nouvelle session utilisateur.
     *
     * Sécurité :
     * - Protection contre l'énumération d'utilisateurs (même message d'erreur)
     * - Protection contre les attaques de timing (toujours exécuter passwordEncoder.matches)
     * - Nettoyage automatique des sessions expirées avant création
     *
     * @param request DTO contenant l'email et le mot de passe
     * @param ipAddress Adresse IP du client
     * @param userAgent User-Agent du client
     * @return DTO contenant les jetons et leurs dates d'expiration
     * @throws ResponseStatusException avec code 401 si les identifiants sont incorrects
     */
    @Transactional
    fun login(request: LoginRequestDto, ipAddress: String, userAgent: String): TokenPairResponseDto {
        val now = Instant.now(clock)
        val normalizedEmail = request.email.normalizeEmail()
        val user = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail)
        
        // Protection contre user enumeration : toujours utiliser le même message d'erreur
        // et toujours exécuter passwordEncoder.matches() pour éviter les attaques de timing
        val dummyHash = "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy" // Hash BCrypt factice
        val userHash = user?.passwordHash ?: dummyHash
        val passwordMatches = passwordEncoder.matches(request.password, userHash)
        
        // Vérifier toutes les conditions d'échec avec le même message
        if (user == null || !passwordMatches || user.status != UserStatus.ACTIVE || user.deletedAt != null) {
            logger.warn("Échec d'authentification pour email: {}, IP: {}", normalizedEmail, ipAddress)
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.INVALID_CREDENTIALS)
        }

        cleanupExpiredSessions(user.id!!, now)

        val tokenPair = issueTokenPair(user, ipAddress, userAgent, now, null)

        user.lastLoginAt = now
        user.updatedAt = now
        
        logger.info("Connexion réussie pour utilisateur: {}, IP: {}", user.id, ipAddress)

        return LoginResponseDto(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            expiresAt = tokenPair.expiresAt,
            refreshExpiresAt = tokenPair.refreshExpiresAt
        )
    }

    /**
     * Renouvelle un couple de jetons à partir d'un refresh token valide.
     *
     * Valide le refresh token, vérifie qu'il n'est pas révoqué ou expiré,
     * puis génère un nouveau couple de jetons. L'ancien refresh token est révoqué
     * et remplacé par le nouveau (rotation des tokens).
     *
     * @param request DTO contenant le refresh token
     * @param ipAddress Adresse IP du client
     * @param userAgent User-Agent du client
     * @return DTO contenant les nouveaux jetons et leurs dates d'expiration
     * @throws ResponseStatusException avec code 401 si le token est invalide, expiré ou révoqué
     */
    @Transactional
    fun refreshToken(request: RefreshTokenRequestDto, ipAddress: String, userAgent: String): TokenPairResponseDto {
        val now = Instant.now(clock)
        val hashed = hashRefreshToken(request.refreshToken)

        val session = userSessionRepository.findByTokenHash(hashed)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.INVALID_TOKEN) }

        if (session.revokedAt != null || session.expiresAt <= now) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.TOKEN_EXPIRED)
        }

        val user = session.user ?: userRepository.findById(session.userId!!)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.INVALID_TOKEN) }

        if (user.deletedAt != null || user.status != UserStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.INVALID_TOKEN)
        }

        cleanupExpiredSessions(user.id!!, now)

        val tokenPair = issueTokenPair(user, ipAddress, userAgent, now, session)

        return tokenPair
    }

    /**
     * Déconnecte un utilisateur en révoquant ses refresh tokens.
     *
     * Révoque le refresh token fourni. Si `revokeAllSessions` est true,
     * toutes les sessions actives de l'utilisateur sont révoquées.
     *
     * @param request DTO contenant le refresh token et l'option de révoquer toutes les sessions
     */
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

    /**
     * Valide un jeton d'accès JWT.
     *
     * Vérifie la signature, l'expiration et la structure du jeton.
     *
     * @param token Le jeton JWT à valider
     * @return DTO contenant le résultat de la validation et les informations du jeton si valide
     */
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

    companion object {
        private const val REFRESH_TOKEN_BYTE_LENGTH = 64
        private const val MAX_IP_LENGTH = 45
        private const val MAX_USER_AGENT_LENGTH = 500
        private const val REVOCATION_REASON_LOGOUT = "LOGOUT"
        private const val REVOCATION_REASON_ROTATION = "ROTATED"
    }
}

