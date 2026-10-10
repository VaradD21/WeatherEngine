package com.weatherengine.backend.user

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant

data class HealthProfileDto(
    val hasAsthma: Boolean = false,
    val hasAllergies: Boolean = false,
    val hasSkinSensitivity: Boolean = false,
    @field:Min(20) @field:Max(500)
    val aqiThreshold: Int = 100,
    @field:Min(1) @field:Max(15)
    val uvThreshold: Int = 6,
    @field:Min(10) @field:Max(100)
    val humidityThreshold: Int = 70,
    val pollenThreshold: String = "MODERATE",
    val alertsEnabled: Boolean = true,
    val updatedAt: Instant? = null
)
