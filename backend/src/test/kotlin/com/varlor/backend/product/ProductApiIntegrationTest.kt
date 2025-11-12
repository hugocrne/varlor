package com.varlor.backend.product

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.varlor.backend.product.model.dto.CreateUserDto
import com.varlor.backend.product.model.dto.LoginRequestDto
import com.varlor.backend.product.model.dto.RegisterRequestDto
import com.varlor.backend.product.model.dto.UpdateUserDto
import com.varlor.backend.product.model.dto.UserDto
import com.varlor.backend.product.model.entity.Client
import com.varlor.backend.product.model.entity.ClientStatus
import com.varlor.backend.product.model.entity.ClientType
import com.varlor.backend.product.model.entity.User
import com.varlor.backend.product.model.entity.UserRole
import com.varlor.backend.product.model.entity.UserStatus
import com.varlor.backend.product.repository.ClientRepository
import com.varlor.backend.product.repository.UserRepository
import com.varlor.backend.product.repository.UserSessionRepository
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductApiIntegrationTest(
    @Autowired private val restTemplate: TestRestTemplate,
    @Autowired private val clientRepository: ClientRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val userSessionRepository: UserSessionRepository,
    @Autowired private val passwordEncoder: PasswordEncoder
) {

    companion object {
        private val dockerAvailable: Boolean = try {
            DockerClientFactory.instance().isDockerAvailable
        } catch (_: Exception) {
            false
        }

        private var postgres: PostgreSQLContainer<Nothing>? = null

        @JvmStatic
        @DynamicPropertySource
        fun configureDataSource(registry: DynamicPropertyRegistry) {
            if (!dockerAvailable) {
                return
            }

            val container = postgres ?: PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
                withDatabaseName("varlor_product_test")
                withUsername("varlor")
                withPassword("varlor")
            }.also {
                it.start()
                postgres = it
            }

            registry.add("spring.datasource.url") { container.jdbcUrl }
            registry.add("spring.datasource.username") { container.username }
            registry.add("spring.datasource.password") { container.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.flyway.locations") { "classpath:db/migration" }
            registry.add("spring.flyway.enabled") { true }
        }
    }

    @LocalServerPort
    private var port: Int = 0

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
    private lateinit var defaultClient: Client
    private lateinit var adminUser: User

    @BeforeAll
    fun ensureDockerAvailable() {
        assumeTrue(
            DockerClientFactory.instance().isDockerAvailable,
            "Docker n'est pas disponible, les tests d'intégration sont ignorés."
        )
    }

    @BeforeEach
    fun setup() {
        userSessionRepository.deleteAll()
        userRepository.deleteAll()
        clientRepository.deleteAll()

        defaultClient = clientRepository.save(
            Client(
                name = "Varlor",
                type = ClientType.COMPANY,
                status = ClientStatus.ACTIVE
            )
        )

        adminUser = userRepository.save(
            User(
                clientId = defaultClient.id,
                email = "owner@varlor.io",
                passwordHash = passwordEncoder.encode("Secret123!"),
                firstName = "Owner",
                lastName = "Admin",
                role = UserRole.OWNER,
                status = UserStatus.ACTIVE
            )
        )
    }

    @AfterEach
    fun cleanup() {
        userSessionRepository.deleteAll()
        userRepository.deleteAll()
        clientRepository.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        postgres?.stop()
        postgres = null
    }

    @Test
    fun `register should create user`() {
        val request = RegisterRequestDto(
            clientId = defaultClient.id!!,
            email = "new.user@varlor.io",
            password = "Password1!",
            firstName = "New",
            lastName = "User",
            role = UserRole.MEMBER,
            status = UserStatus.ACTIVE
        )

        val response = restTemplate.postForEntity(
            url("/api/auth/register"),
            HttpEntity(request, jsonHeaders()),
            UserDto::class.java
        )

        assertThat(response.statusCode.value()).isEqualTo(201)
        val body = response.body!!
        assertThat(body.email).isEqualTo("new.user@varlor.io")
        assertThat(body.role).isEqualTo(UserRole.MEMBER)
    }

    @Test
    fun `login should return tokens`() {
        val response = login(adminUser.email, "Secret123!")
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body!!.accessToken).isNotBlank
        assertThat(response.body!!.refreshToken).isNotBlank
    }

    @Test
    fun `refresh should return new token pair`() {
        val loginResponse = login(adminUser.email, "Secret123!").body!!

        val refreshRequest = mapOf("refreshToken" to loginResponse.refreshToken)
        val refreshResponse = restTemplate.postForEntity(
            url("/api/auth/refresh"),
            HttpEntity(refreshRequest, jsonHeaders()),
            TokenPairResponse::class.java
        )

        assertThat(refreshResponse.statusCode.value()).isEqualTo(200)
        val tokens = refreshResponse.body!!
        assertThat(tokens.accessToken).isNotBlank
        assertThat(tokens.refreshToken).isNotBlank
        assertThat(tokens.refreshToken).isNotEqualTo(loginResponse.refreshToken)
    }

    @Test
    fun `protected route without token should return 401`() {
        val response = restTemplate.exchange(
            url("/api/users"),
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertThat(response.statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `user CRUD flow`() {
        val loginResponse = login(adminUser.email, "Secret123!").body!!
        val headers = authorizedHeaders(loginResponse.accessToken)

        val createRequest = CreateUserDto(
            clientId = defaultClient.id!!,
            email = "member@varlor.io",
            password = "Password123!",
            firstName = "Varlor",
            lastName = "Member",
            role = UserRole.MEMBER,
            status = UserStatus.ACTIVE
        )

        val createdResponse = restTemplate.postForEntity(
            url("/api/users"),
            HttpEntity(createRequest, headers),
            UserDto::class.java
        )
        assertThat(createdResponse.statusCode.value()).isEqualTo(201)
        val createdUser = createdResponse.body!!

        val listResponse: ResponseEntity<String> = restTemplate.exchange(
            url("/api/users"),
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            String::class.java
        )
        assertThat(listResponse.statusCode.value()).isEqualTo(200)
        val users: List<UserDto> = objectMapper.readValue(listResponse.body!!)
        assertThat(users).extracting<String> { it.email }.contains("member@varlor.io")

        val detailResponse = restTemplate.exchange(
            url("/api/users/${createdUser.id}"),
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            UserDto::class.java
        )
        assertThat(detailResponse.statusCode.value()).isEqualTo(200)
        assertThat(detailResponse.body!!.email).isEqualTo("member@varlor.io")

        val updateRequest = UpdateUserDto(firstName = "Updated")
        val updateResponse = restTemplate.exchange(
            url("/api/users/${createdUser.id}"),
            HttpMethod.PATCH,
            HttpEntity(updateRequest, headers),
            UserDto::class.java
        )
        assertThat(updateResponse.statusCode.value()).isEqualTo(200)
        assertThat(updateResponse.body!!.firstName).isEqualTo("Updated")

        val deleteResponse = restTemplate.exchange(
            url("/api/users/${createdUser.id}"),
            HttpMethod.DELETE,
            HttpEntity<Void>(headers),
            String::class.java
        )
        assertThat(deleteResponse.statusCode.value()).isEqualTo(204)

        val notFoundResponse = restTemplate.exchange(
            url("/api/users/${createdUser.id}"),
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            String::class.java
        )
        assertThat(notFoundResponse.statusCode.value()).isEqualTo(404)
    }

    private fun login(email: String, password: String): ResponseEntity<TokenPairResponse> {
        val request = LoginRequestDto(email = email, password = password)
        return restTemplate.postForEntity(
            url("/api/auth/login"),
            HttpEntity(request, jsonHeaders()),
            TokenPairResponse::class.java
        )
    }

    private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
    }

    private fun authorizedHeaders(token: String): HttpHeaders =
        jsonHeaders().apply { setBearerAuth(token) }

    private fun url(path: String) = "http://localhost:$port$path"

    data class TokenPairResponse(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: String,
        val refreshExpiresAt: String
    )
}

