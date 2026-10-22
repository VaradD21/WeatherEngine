package com.weatherengine.backend.config

import com.weatherengine.backend.auth.JwtService
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource

class JwtSecretValidationTest {

    @Test
    fun `fails fast if jwt secret is empty`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            JwtService(secret = "", expirationMs = 86400000)
        }
        assertTrue(ex.message!!.contains("must not be blank") || ex.message!!.contains("at least 32 bytes"))
    }

    @Test
    fun `fails fast if jwt secret is less than 32 bytes`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            JwtService(secret = "short-secret-12345", expirationMs = 86400000)
        }
        assertTrue(ex.message!!.contains("at least 32 bytes"))
    }

    @Test
    fun `context fails to start with prod profile and no secret`() {
        assertThrows(Exception::class.java) {
            val context = AnnotationConfigApplicationContext()
            context.environment.activeProfiles
            context.environment.propertySources.addFirst(
                MapPropertySource("prod-no-secret", mapOf("app.jwt.secret" to ""))
            )
            context.register(JwtService::class.java)
            context.refresh()
        }
    }
}
