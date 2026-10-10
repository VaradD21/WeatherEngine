package com.weatherengine.app.data.model

import com.weatherengine.app.data.openmeteo.OpenMeteoAirQualityResponse
import com.weatherengine.app.data.openmeteo.OpenMeteoForecastResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val username: String? = null,
    val phoneNumber: String? = null
)

@Serializable
data class HealthProfileDto(
    val hasAsthma: Boolean = false,
    val hasAllergies: Boolean = false,
    val hasSkinSensitivity: Boolean = false,
    val aqiThreshold: Int = 100,
    val uvThreshold: Int = 6,
    val humidityThreshold: Int = 70,
    val pollenThreshold: String = "MODERATE",
    val alertsEnabled: Boolean = true
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val userId: Long? = null,
    val email: String,
    val token: String
)

@Serializable
data class HomepageResponse(
    val widgets: List<WidgetDto> = emptyList(),
    val message: String? = null
)

@Serializable
data class WidgetDto(
    val type: String,
    val data: JsonElement
)

@Serializable
data class WeatherBundleDto(
    val forecast: OpenMeteoForecastResponse,
    val airQuality: OpenMeteoAirQualityResponse? = null,
    val fetchedAtMs: Long = 0L
)

@Serializable
data class PersonaDto(
    val code: String,
    val displayName: String
)

@Serializable
data class SetPersonasRequest(
    val personaCodes: List<String>
)
