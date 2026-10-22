package com.weatherengine.backend.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.Persona
import com.weatherengine.backend.persona.PersonaRepository
import com.weatherengine.backend.persona.UserPersona
import com.weatherengine.backend.persona.UserPersonaRepository
import com.weatherengine.backend.user.User
import com.weatherengine.backend.user.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*

class AuthSecurityTest {

    private val userRepository = mockk<UserRepository>()
    private val personaRepository = mockk<PersonaRepository>()
    private val userPersonaRepository = mockk<UserPersonaRepository>(relaxed = true)
    private val passwordEncoder = BCryptPasswordEncoder()
    private val secretKey = "test-secret-key-must-be-at-least-32-bytes-long-12345"
    private val jwtService = JwtService(secret = secretKey, expirationMs = 3_600_000L)

    private val authService = AuthService(
        userRepository = userRepository,
        personaRepository = personaRepository,
        userPersonaRepository = userPersonaRepository,
        passwordEncoder = passwordEncoder,
        jwtService = jwtService
    )

    @BeforeEach
    fun resetMocks() {
        clearAllMocks()
    }

    // 1.1 Register valid user
    @Test
    fun `1_1 - Register userA with valid data returns 201 equivalent with token and hashed password`() {
        val email = "usera@example.com"
        val rawPassword = "SecurePassword123!"
        every { userRepository.existsByEmail(email) } returns false
        val capturedUser = slot<User>()
        every { userRepository.save(capture(capturedUser)) } answers {
            User(id = UUID.randomUUID(), email = capturedUser.captured.email, passwordHash = capturedUser.captured.passwordHash)
        }
        every { personaRepository.findByCode("health_conscious") } returns Persona(id = 1, code = "health_conscious", displayName = "Health-conscious")
        every { userPersonaRepository.save(any()) } answers { firstArg() }

        val res = authService.signup(SignupRequest(email = email, password = rawPassword))

        assertNotNull(res.token)
        assertEquals(email, res.email)
        // Verify password is NOT plaintext
        assertNotEquals(rawPassword, capturedUser.captured.passwordHash)
        assertTrue(passwordEncoder.matches(rawPassword, capturedUser.captured.passwordHash))
    }

    // 1.2 Register duplicate email -> Conflict
    @Test
    fun `1_2 - Register duplicate email is rejected with clean Conflict 409 exception`() {
        val email = "duplicate@example.com"
        every { userRepository.existsByEmail(email) } returns true

        val ex = assertThrows(ApiException::class.java) {
            authService.signup(SignupRequest(email = email, password = "Password123!"))
        }
        assertEquals(HttpStatus.CONFLICT, ex.status)
        assertEquals("Email is already registered", ex.message)
    }

    // 1.4 Password never returned in AuthResponse
    @Test
    fun `1_4 - AuthResponse never leaks password hash or plaintext`() {
        val mapper = ObjectMapper()
        val authRes = AuthResponse(email = "test@example.com", token = "jwt.sample.token")
        val json = mapper.writeValueAsString(authRes)

        assertFalse(json.contains("password"))
        assertFalse(json.contains("hash"))
    }

    // 1.6 Login wrong password -> 401 generic
    @Test
    fun `1_6 - Login wrong password returns 401 with generic message`() {
        val email = "user@example.com"
        val correctHash = passwordEncoder.encode("correctPass123")
        every { userRepository.findByEmail(email) } returns User(
            id = UUID.randomUUID(),
            email = email,
            passwordHash = correctHash
        )

        val ex = assertThrows(ApiException::class.java) {
            authService.login(LoginRequest(email = email, password = "wrongPassword123"))
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status)
        assertEquals("Invalid email or password", ex.message)
    }

    // 1.7 Login non-existent user -> identical 401 (no user enumeration)
    @Test
    fun `1_7 - Login non-existent user returns exact same 401 message to prevent enumeration`() {
        val email = "nonexistent@example.com"
        every { userRepository.findByEmail(email) } returns null

        val ex = assertThrows(ApiException::class.java) {
            authService.login(LoginRequest(email = email, password = "anyPassword123"))
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status)
        assertEquals("Invalid email or password", ex.message)
    }

    // 1.8 Expired token -> parse returns null
    @Test
    fun `1_8 - Expired token fails parsing and returns null`() {
        val shortLivedJwtService = JwtService(secret = secretKey, expirationMs = -1000L) // already expired
        val token = shortLivedJwtService.generateToken(UUID.randomUUID(), "expired@example.com")

        val parsed = jwtService.parseToken(token)
        assertNull(parsed, "Expired token must not parse successfully")
    }

    // 1.9 Tampered token signature -> parse returns null
    @Test
    fun `1_9 - Tampered token fails signature verification and returns null`() {
        val validToken = jwtService.generateToken(UUID.randomUUID(), "legit@example.com")
        val parts = validToken.split(".")
        assertEquals(3, parts.size)

        // Tamper with payload
        val tamperedPayload = Base64.getUrlEncoder().encodeToString("""{"sub":"123","email":"hacker@evil.com"}""".toByteArray())
        val tamperedToken = "${parts[0]}.$tamperedPayload.${parts[2]}"

        val parsed = jwtService.parseToken(tamperedToken)
        assertNull(parsed, "Tampered JWT payload or signature must return null")
    }

    // SQL Injection payload handled safely
    @Test
    fun `1_3 - SQL Injection strings in email or password treated as literal string values`() {
        val sqlInjectionEmail = "' OR '1'='1' --"
        every { userRepository.existsByEmail(sqlInjectionEmail.trim().lowercase()) } returns false
        val capturedUser = slot<User>()
        every { userRepository.save(capture(capturedUser)) } answers {
            User(id = UUID.randomUUID(), email = capturedUser.captured.email, passwordHash = capturedUser.captured.passwordHash)
        }
        every { personaRepository.findByCode("health_conscious") } returns null

        val res = authService.signup(SignupRequest(email = sqlInjectionEmail, password = "Password123!"))
        assertEquals(sqlInjectionEmail.trim().lowercase(), res.email)
        assertEquals(sqlInjectionEmail.trim().lowercase(), capturedUser.captured.email)
    }
}
