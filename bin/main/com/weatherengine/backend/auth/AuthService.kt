package com.weatherengine.backend.auth

import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.PersonaRepository
import com.weatherengine.backend.persona.UserPersona
import com.weatherengine.backend.persona.UserPersonaId
import com.weatherengine.backend.persona.UserPersonaRepository
import com.weatherengine.backend.user.User
import com.weatherengine.backend.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val personaRepository: PersonaRepository,
    private val userPersonaRepository: UserPersonaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    @Transactional
    fun signup(request: SignupRequest): AuthResponse {
        val normalizedEmail = request.email.trim().lowercase()
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ApiException(HttpStatus.CONFLICT, "Email is already registered")
        }

        val savedUser = userRepository.save(
            User(
                email = normalizedEmail,
                passwordHash = passwordEncoder.encode(request.password)
            )
        )

        val userId = savedUser.id
            ?: throw ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist user")

        // Assign default persona ("health_conscious") on signup so homepage has immediate widgets
        personaRepository.findByCode("health_conscious")?.let { defaultPersona ->
            val personaId = defaultPersona.id
            if (personaId != null) {
                userPersonaRepository.save(
                    UserPersona(
                        id = UserPersonaId(userId = userId, personaId = personaId),
                        user = savedUser,
                        persona = defaultPersona
                    )
                )
            }
        }

        val token = jwtService.generateToken(userId = userId, email = savedUser.email)
        return AuthResponse(email = savedUser.email, token = token)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val normalizedEmail = request.email.trim().lowercase()
        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        }

        val userId = user.id
            ?: throw ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid user record")

        val token = jwtService.generateToken(userId = userId, email = user.email)
        return AuthResponse(email = user.email, token = token)
    }
}
