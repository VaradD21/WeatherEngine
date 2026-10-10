package com.weatherengine.backend.auth

import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.PersonaRepository
import com.weatherengine.backend.persona.UserPersona
import com.weatherengine.backend.persona.UserPersonaId
import com.weatherengine.backend.persona.UserPersonaRepository
import com.weatherengine.backend.user.User
import com.weatherengine.backend.user.UserRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

data class AuthenticatedUser(val userId: UUID, val email: String)

data class SignupRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email address format")
    val email: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be at least 8 characters")
    val password: String,
    val username: String? = null,
    val phoneNumber: String? = null
)

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email address format")
    val email: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be at least 8 characters")
    val password: String
)

data class AuthResponse(val email: String, val token: String)

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms:86400000}") private val expirationMs: Long
) {
    private val signingKey: SecretKey by lazy {
        val keyBytes = secret.toByteArray(StandardCharsets.UTF_8)
        require(keyBytes.size >= 32) { "JWT_SECRET must be at least 32 bytes" }
        Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(userId: UUID, email: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()
    }

    fun parseToken(token: String): AuthenticatedUser? {
        return try {
            val claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).payload
            val subject = claims.subject ?: return null
            val email = claims["email"] as? String ?: return null
            AuthenticatedUser(userId = UUID.fromString(subject), email = email)
        } catch (_: Exception) {
            null
        }
    }
}

@Component
class JwtAuthenticationFilter(private val jwtService: JwtService) : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val token = req.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("Bearer ") }?.substring(7)?.trim()
        if (!token.isNullOrEmpty() && SecurityContextHolder.getContext().authentication == null) {
            jwtService.parseToken(token)?.let { user ->
                SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                    user, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
                ).apply { details = WebAuthenticationDetailsSource().buildDetails(req) }
            }
        }
        chain.doFilter(req, res)
    }
}

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {
    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))
}

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
        val email = request.email.trim().lowercase()
        if (userRepository.existsByEmail(email)) {
            throw ApiException(HttpStatus.CONFLICT, "Email is already registered")
        }
        val user = userRepository.save(
            User(
                email = email,
                passwordHash = passwordEncoder.encode(request.password),
                username = request.username?.trim()?.takeIf { it.isNotEmpty() },
                phoneNumber = request.phoneNumber?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
        personaRepository.findByCode("health_conscious")?.let { defaultPersona ->
            userPersonaRepository.save(
                UserPersona(UserPersonaId(user.id!!, defaultPersona.id!!), user, defaultPersona)
            )
        }
        return AuthResponse(user.email, jwtService.generateToken(user.id!!, user.email))
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        val user = userRepository.findByEmail(email)
            ?.takeIf { passwordEncoder.matches(request.password, it.passwordHash) }
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        return AuthResponse(user.email, jwtService.generateToken(user.id!!, user.email))
    }
}
