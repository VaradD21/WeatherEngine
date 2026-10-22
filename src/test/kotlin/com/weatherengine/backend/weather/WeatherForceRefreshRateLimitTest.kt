package com.weatherengine.backend.weather

import com.weatherengine.backend.auth.JwtService
import org.junit.jupiter.api.BeforeEach
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
class WeatherForceRefreshRateLimitTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setup() {
        WeatherController.resetRateLimitsForTesting()
    }

    @Test
    fun `forceRefresh rate limit returns 429 when invoked twice within cooldown`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId, "forcerefresh@example.com", "USER")

        // First call with forceRefresh=true succeeds
        mockMvc.perform(
            get("/api/weather")
                .param("lat", "28.61")
                .param("lon", "77.20")
                .param("forceRefresh", "true")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)

        // Second call within 5 seconds gets 429 Too Many Requests
        mockMvc.perform(
            get("/api/weather")
                .param("lat", "28.61")
                .param("lon", "77.20")
                .param("forceRefresh", "true")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isTooManyRequests)
    }
}
