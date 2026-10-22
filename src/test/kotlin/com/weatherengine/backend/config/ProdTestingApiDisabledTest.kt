package com.weatherengine.backend.config

import com.weatherengine.backend.auth.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = [
    "app.jwt.secret=this-is-a-valid-production-secret-key-of-32-chars",
    "spring.jpa.hibernate.ddl-auto=none"
])
class ProdTestingApiDisabledTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `testing api endpoint does not exist in prod profile and returns 404`() {
        val adminToken = jwtService.generateToken(UUID.randomUUID(), "admin@example.com", "ADMIN")
        mockMvc.perform(
            get("/api/testing/overrides")
                .header("Authorization", "Bearer $adminToken")
        ).andExpect(status().isNotFound)
    }
}
