package com.weatherengine.backend.config

import com.weatherengine.backend.auth.JwtAuthenticationFilter
import com.weatherengine.backend.security.RestAuthenticationEntryPoint
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val authenticationEntryPoint: RestAuthenticationEntryPoint,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {
    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .exceptionHandling { it.authenticationEntryPoint(authenticationEntryPoint) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/auth/**", "/api/weather", "/actuator/health", "/error").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        val patterns = mutableListOf(
            "http://localhost:8080",
            "http://10.0.2.2:8080"
        )

        val envOrigins = System.getenv("ALLOWED_ORIGINS")
        if (envOrigins.isNullOrBlank()) {
            logger.warn(
                "ALLOWED_ORIGINS environment variable is not set. Falling back to default origins: {}",
                patterns
            )
        } else {
            val parsedOrigins = envOrigins
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (parsedOrigins.isNotEmpty()) {
                patterns.addAll(parsedOrigins)
            } else {
                logger.warn(
                    "ALLOWED_ORIGINS environment variable contained no valid entries. Falling back to default origins: {}",
                    patterns
                )
            }
        }

        configuration.allowedOriginPatterns = patterns
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
