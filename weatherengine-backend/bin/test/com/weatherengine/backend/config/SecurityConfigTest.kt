package com.weatherengine.backend.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.cors.CorsConfigurationSource

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var corsConfigurationSource: CorsConfigurationSource

    @Test
    fun `api auth endpoints are publicly accessible`() {
        mockMvc.perform(get("/api/auth/test"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("public-auth-success"))
    }

    @Test
    fun `actuator health endpoint is publicly accessible`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `error endpoint is publicly accessible`() {
        mockMvc.perform(get("/error"))
            .andExpect { result ->
                assertNotEquals(401, result.response.status)
            }
    }

    @Test
    fun `protected api endpoint returns 401 JSON when unauthenticated`() {
        mockMvc.perform(get("/api/data/test"))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Valid authentication token required"))
    }

    @Test
    fun `unmatched endpoints are denied by default`() {
        mockMvc.perform(get("/other/unprotected"))
            .andExpect { result ->
                val status = result.response.status
                assertTrue(
                    status == 401 || status == 403,
                    "Expected 401 or 403 for denied endpoint, but got: $status"
                )
            }
    }

    @Test
    fun `CORS does not permit wildcard origins and requires explicit patterns`() {
        val request = MockHttpServletRequest()
        val corsConfig = corsConfigurationSource.getCorsConfiguration(request)
        assertNotNull(corsConfig)

        val allowedPatterns = corsConfig?.allowedOriginPatterns ?: emptyList()
        assertFalse(allowedPatterns.contains("*"), "Must not contain bare wildcard origin pattern")
        assertNull(corsConfig?.allowedOrigins, "allowedOrigins must not be set with wildcard")
        assertTrue(corsConfig?.allowCredentials == true, "allowCredentials must be true")
        assertTrue(allowedPatterns.contains("http://localhost:8080"), "Must include localhost:8080")
        assertTrue(allowedPatterns.contains("http://10.0.2.2:8080"), "Must include 10.0.2.2:8080")
    }

    @Test
    fun `CORS preflight request from allowed origin succeeds`() {
        mockMvc.perform(
            options("/api/data/test")
                .header("Origin", "http://localhost:8080")
                .header("Access-Control-Request-Method", "GET")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"))
    }

    @Test
    fun `single SecurityFilterChain and single PasswordEncoder bean exist without duplicates`() {
        val filterChainBeans = context.getBeansOfType(SecurityFilterChain::class.java)
        assertEquals(1, filterChainBeans.size, "Exactly one SecurityFilterChain bean must exist")

        val passwordEncoderBeans = context.getBeansOfType(PasswordEncoder::class.java)
        assertEquals(1, passwordEncoderBeans.size, "Exactly one PasswordEncoder bean must exist")
    }
}
