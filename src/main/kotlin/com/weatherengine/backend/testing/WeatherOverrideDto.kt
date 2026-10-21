package com.weatherengine.backend.testing

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherOverrideDto(
    val id: String = UUID.randomUUID().toString(),
    val label: String? = null,
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 10.0,
    val targetUserId: UUID? = null,
    val active: Boolean = true,

    // Air Quality Overrides
    val usAqi: Int? = null,
    val pm25: Double? = null,
    val grassPollen: Double? = null,
    val birchPollen: Double? = null,
    val alderPollen: Double? = null,
    val ragweedPollen: Double? = null,

    // Current Weather Overrides
    val temperature2m: Double? = null,
    val apparentTemperature: Double? = null,
    val relativeHumidity2m: Int? = null,
    val weatherCode: Int? = null,
    val windSpeed10m: Double? = null,
    val windGusts10m: Double? = null,
    val precipitation: Double? = null,
    val isDay: Int? = null,

    // Hourly / Daily / Atmospheric Overrides
    val precipitationProbability: Int? = null,
    val uvIndex: Double? = null,
    val visibility: Double? = null,

    // Marine Overrides
    val waveHeight: Double? = null,
    val seaSurfaceTemperature: Double? = null,

    val createdAtMs: Long = System.currentTimeMillis()
)

data class PresetApplyRequest(
    val presetKey: String,
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 10.0,
    val targetUserId: UUID? = null,
    val label: String? = null
)

data class NotificationEvaluateRequest(
    val latitude: Double,
    val longitude: Double,
    val userId: UUID? = null
)

data class NotificationTriggerRequest(
    val latitude: Double,
    val longitude: Double,
    val userId: UUID? = null,
    val alertType: String? = null,
    val customHeadline: String? = null,
    val customMessage: String? = null,
    val severity: String = "WARNING"
)

data class UserSummaryDto(
    val id: UUID,
    val email: String,
    val username: String?,
    val personas: List<String>,
    val aqiThreshold: Int,
    val uvThreshold: Int,
    val alertsEnabled: Boolean
)
