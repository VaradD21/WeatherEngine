package com.weatherengine.backend.auth

import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.Persona
import com.weatherengine.backend.persona.PersonaRepository
import com.weatherengine.backend.persona.UserPersonaRepository
import com.weatherengine.backend.user.User
import com.weatherengine.backend.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.UUID

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val personaRepository = mockk<PersonaRepository>()
    private val userPersonaRepository = mockk<UserPersonaRepository>(relaxed = true)
    private val passwordEncoder = BCryptPasswordEncoder()
    private val jwtService = JwtService(
        secret = "test-secret-key-must-be-at-least-32-bytes-long-12345",
        expirationMs = 3_600_000L
    )

    private val authService = AuthService(
        userRepository = userRepository,
        personaRepository = personaRepository,
        userPersonaRepository = userPersonaRepository,
        passwordEncoder = passwordEncoder,
        jwtService = jwtService
    )

    @Test
    fun `signup creates user, assigns default persona, and returns valid JWT`() {
        val userId = UUID.randomUUID()
        every { userRepository.existsByEmail("user@example.com") } returns false
        every { userRepository.save(any()) } answers {
            val u = firstArg<User>()
            User(id = userId, email = u.email, passwordHash = u.passwordHash)
        }
        every { personaRepository.findByCode("health_conscious") } returns Persona(
            id = 1,
            code = "health_conscious",
            displayName = "Health-conscious"
        )

        val response = authService.signup(SignupRequest("User@Example.com", "password123"))

        assertEquals("user@example.com", response.email)
        val parsed = jwtService.parseToken(response.token)
        assertNotNull(parsed)
        assertEquals(userId, parsed?.userId)
        assertEquals("user@example.com", parsed?.email)
        verify(exactly = 1) { userPersonaRepository.save(any()) }
    }

    @Test
    fun `signup throws 409 CONFLICT when email already exists`() {
        every { userRepository.existsByEmail("dup@example.com") } returns true

        val ex = assertThrows(ApiException::class.java) {
            authService.signup(SignupRequest("dup@example.com", "password123"))
        }
        assertEquals(HttpStatus.CONFLICT, ex.status)
    }

    @Test
    fun `login succeeds with valid credentials and fails with wrong password`() {
        val userId = UUID.randomUUID()
        val hash = passwordEncoder.encode("correctPass123")
        every { userRepository.findByEmail("user@example.com") } returns User(
            id = userId,
            email = "user@example.com",
            passwordHash = hash
        )

        val success = authService.login(LoginRequest("user@example.com", "correctPass123"))
        assertEquals("user@example.com", success.email)
        assertNotNull(jwtService.parseToken(success.token))

        val ex = assertThrows(ApiException::class.java) {
            authService.login(LoginRequest("user@example.com", "wrongPass123"))
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status)
    }
}
