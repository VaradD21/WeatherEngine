package com.weatherengine.backend.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "app.cors.allowed-origins=https://allowed-weather.example.com,https://app.mausam.io"
])
class CorsSecurityTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `allowed origin receives cors headers on preflight`() {
        mockMvc.perform(
            options("/api/weather")
                .header("Origin", "https://allowed-weather.example.com")
                .header("Access-Control-Request-Method", "GET")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "https://allowed-weather.example.com"))
    }

    @Test
    fun `disallowed origin does not receive cors headers`() {
        mockMvc.perform(
            options("/api/weather")
                .header("Origin", "https://malicious-site.example.com")
                .header("Access-Control-Request-Method", "GET")
        )
            .andExpect(status().isForbidden)
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
    }
}
