package com.varlor.backend.product.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.util.StringUtils
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.Locale

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtDecoder: JwtDecoder,
    @Value("\${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private val allowedOriginsString: String,
    @Value("\${app.swagger.enabled:true}")
    private val swaggerEnabled: Boolean
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun roleHierarchy(): RoleHierarchy {
        val hierarchy = "ROLE_OWNER > ROLE_ADMIN > ROLE_MEMBER > ROLE_SERVICE"
        // Note: Utilisation de l'API dépréciée car la nouvelle API n'est pas encore disponible dans Spring Security 6.x
        @Suppress("DEPRECATION")
        val roleHierarchyImpl = RoleHierarchyImpl()
        @Suppress("DEPRECATION")
        roleHierarchyImpl.setHierarchy(hierarchy)
        return roleHierarchyImpl
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOriginsString.split(",").map { it.trim() }
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF désactivé car API stateless avec JWT (protection via SameSite cookies si nécessaire)
            // Note: Pour une protection supplémentaire, considérer l'implémentation d'un double submit cookie
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { headers ->
                headers
                    .frameOptions { it.deny() }
                    .contentTypeOptions { }
                    .httpStrictTransportSecurity { hsts ->
                        hsts
                            .maxAgeInSeconds(31536000)
                            .includeSubDomains(true)
                    }
                    .xssProtection { }
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        AntPathRequestMatcher("/api/auth/**"),
                        AntPathRequestMatcher("/actuator/health")
                    ).permitAll()
                    .apply {
                        // Swagger UI uniquement si activé et en développement
                        if (swaggerEnabled) {
                            requestMatchers(
                                AntPathRequestMatcher("/swagger-ui/**"),
                                AntPathRequestMatcher("/v3/api-docs/**")
                            ).hasAnyRole("ADMIN", "OWNER")
                        } else {
                            requestMatchers(
                                AntPathRequestMatcher("/swagger-ui/**"),
                                AntPathRequestMatcher("/v3/api-docs/**")
                            ).denyAll()
                        }
                    }
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwtConfigurer ->
                    jwtConfigurer.decoder(jwtDecoder)
                    jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }

        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): Converter<Jwt, AbstractAuthenticationToken> =
        object : Converter<Jwt, AbstractAuthenticationToken> {
            override fun convert(jwt: Jwt): AbstractAuthenticationToken {
                val roleClaim = jwt.getClaimAsString("role")
                val authorities = if (StringUtils.hasText(roleClaim)) {
                    listOf(SimpleGrantedAuthority("ROLE_${roleClaim.uppercase(Locale.getDefault())}"))
                } else {
                    emptyList()
                }
                return JwtAuthenticationToken(jwt, authorities, jwt.subject)
            }
        }
}

