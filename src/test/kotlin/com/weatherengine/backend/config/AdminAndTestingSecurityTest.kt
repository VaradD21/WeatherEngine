package com.weatherengine.backend.config

import com.weatherengine.backend.auth.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAndTestingSecurityTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `anonymous request to admin endpoint returns 401`() {
        mockMvc.perform(get("/admin"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `anonymous request to testing endpoint returns 401`() {
        mockMvc.perform(get("/api/testing/overrides"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `normal user request to admin endpoint returns 403`() {
        val userToken = jwtService.generateToken(UUID.randomUUID(), "user@example.com", "USER")
        mockMvc.perform(
            get("/admin")
                .header("Authorization", "Bearer $userToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `normal user request to testing endpoint returns 403`() {
        val userToken = jwtService.generateToken(UUID.randomUUID(), "user@example.com", "USER")
        mockMvc.perform(
            get("/api/testing/overrides")
                .header("Authorization", "Bearer $userToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `admin user request to admin endpoint returns 200`() {
        val adminToken = jwtService.generateToken(UUID.randomUUID(), "admin@example.com", "ADMIN")
        mockMvc.perform(
            get("/admin")
                .header("Authorization", "Bearer $adminToken")
        ).andExpect(status().isOk)
    }

    @Test
    fun `admin user request to testing endpoint returns 200 in test or dev profile`() {
        val adminToken = jwtService.generateToken(UUID.randomUUID(), "admin@example.com", "ADMIN")
        mockMvc.perform(
            get("/api/testing/overrides")
                .header("Authorization", "Bearer $adminToken")
        ).andExpect(status().isOk)
    }
}
