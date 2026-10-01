package com.weatherengine.backend.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

data class AuthenticatedUser(
    val userId: UUID,
    val email: String
)

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms:86400000}") private val expirationMs: Long
) {
    private val signingKey: SecretKey by lazy {
        val keyBytes = secret.toByteArray(StandardCharsets.UTF_8)
        require(keyBytes.size >= 32) {
            "JWT_SECRET must be at least 32 bytes (256 bits) for HMAC-SHA256"
        }
        Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(userId: UUID, email: String): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()
    }

    fun parseToken(token: String): AuthenticatedUser? {
        return try {
            val claims: Claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val subject = claims.subject ?: return null
            val email = claims["email"] as? String ?: return null
            val userId = UUID.fromString(subject)
            AuthenticatedUser(userId = userId, email = email)
        } catch (_: Exception) {
            null
        }
    }
}
