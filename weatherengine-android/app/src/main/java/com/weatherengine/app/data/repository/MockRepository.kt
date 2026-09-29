package com.weatherengine.app.data.repository

import com.weatherengine.app.core.Validators
import com.weatherengine.app.data.api.FailureKind
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.model.AuthResponse
import com.weatherengine.app.data.model.HomepageResponse
import com.weatherengine.app.data.model.PersonaDto
import com.weatherengine.app.data.model.WidgetDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MockRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : WeatherEngineRepository {

    private val registeredUsers = mutableMapOf(
        "test@example.com" to "password123"
    )

    private val validPersonas = mapOf(
        "health_conscious" to "Health-conscious",
        "outdoor_fitness" to "Outdoor Fitness",
        "commuter" to "Commuter"
    )

    private val personaWidgets = mapOf(
        "health_conscious" to listOf("aqi_card", "uv_index_card", "heat_alert_card"),
        "outdoor_fitness" to listOf("aqi_card", "uv_index_card", "wind_speed_card", "storm_fog_alert_card", "humidity_card"),
        "commuter" to listOf("visibility_card", "traffic_card", "storm_fog_alert_card")
    )

    private var selectedPersonas = mutableListOf(
        PersonaDto("health_conscious", "Health-conscious")
    )

    override suspend fun signup(email: String, password: String): NetworkResult<AuthResponse> = withContext(dispatcher) {
        delay(300)
        if (!Validators.isValidEmail(email)) {
            return@withContext NetworkResult.Failure(FailureKind.Client, "Invalid email address format")
        }
        if (!Validators.isValidPassword(password)) {
            return@withContext NetworkResult.Failure(FailureKind.Client, "Password must be at least 8 characters")
        }
        if (registeredUsers.containsKey(email.lowercase())) {
            return@withContext NetworkResult.Failure(FailureKind.Client, "Email is already registered")
        }
        registeredUsers[email.lowercase()] = password
        NetworkResult.Success(
            AuthResponse(
                userId = registeredUsers.size.toLong(),
                email = email,
                token = "mock.jwt.token.${System.currentTimeMillis()}"
            )
        )
    }

    override suspend fun login(email: String, password: String): NetworkResult<AuthResponse> = withContext(dispatcher) {
        delay(300)
        if (!Validators.isValidEmail(email) || !Validators.isValidPassword(password)) {
            return@withContext NetworkResult.Failure(FailureKind.Client, "Invalid email or password format")
        }
        val storedPass = registeredUsers[email.lowercase()]
        if (storedPass == null || storedPass != password) {
            return@withContext NetworkResult.Failure(FailureKind.Unauthorized, "Invalid email or password")
        }
        NetworkResult.Success(
            AuthResponse(
                userId = 1L,
                email = email,
                token = "mock.jwt.token.${System.currentTimeMillis()}"
            )
        )
    }

    override suspend fun getPersonas(): NetworkResult<List<PersonaDto>> = withContext(dispatcher) {
        delay(300)
        NetworkResult.Success(selectedPersonas.toList())
    }

    override suspend fun setPersonas(codes: List<String>): NetworkResult<List<PersonaDto>> = withContext(dispatcher) {
        delay(300)
        for (code in codes) {
            if (!validPersonas.containsKey(code)) {
                return@withContext NetworkResult.Failure(FailureKind.Client, "Invalid persona code: $code")
            }
        }
        val updated = codes.distinct().map { code ->
            PersonaDto(code, validPersonas[code] ?: code)
        }
        selectedPersonas.clear()
        selectedPersonas.addAll(updated)
        NetworkResult.Success(selectedPersonas.toList())
    }

    override suspend fun getHomepage(lat: Double, lon: Double): NetworkResult<HomepageResponse> = withContext(dispatcher) {
        delay(300)
        if (selectedPersonas.isEmpty()) {
            return@withContext NetworkResult.Success(
                HomepageResponse(widgets = emptyList(), message = "No personas selected yet")
            )
        }

        val allWidgetTypes = mutableListOf<String>()
        selectedPersonas.forEach { persona ->
            personaWidgets[persona.code]?.let { allWidgetTypes.addAll(it) }
        }
        val distinctWidgetTypes = allWidgetTypes.distinct()

        val widgets = distinctWidgetTypes.map { type ->
            WidgetDto(type = type, data = sampleDataFor(type))
        }

        NetworkResult.Success(HomepageResponse(widgets = widgets))
    }

    private fun sampleDataFor(type: String): JsonObject = when (type) {
        "aqi_card" -> buildJsonObject {
            put("aqi", 2)
            put("category", "Fair")
        }
        "humidity_card" -> buildJsonObject {
            put("humidityPercent", 68)
        }
        "uv_index_card" -> buildJsonObject {
            put("status", "unavailable")
            put("message", "UV sensor index currently offline")
        }
        "sunrise_sunset_card" -> buildJsonObject {
            put("sunrise", "2026-09-28T06:15:00Z")
            put("sunset", "2026-09-28T18:30:00Z")
        }
        "wind_speed_card" -> buildJsonObject {
            put("speedMetersPerSecond", 5.2)
        }
        "heat_alert_card" -> buildJsonObject {
            put("alertActive", false)
            put("temperatureCelsius", 28.5)
            put("message", "No active heat alerts for your area")
        }
        "traffic_card" -> buildJsonObject {
            put("status", "mocked")
            put("message", "Demo traffic simulation pending integration")
        }
        "visibility_card" -> buildJsonObject {
            put("visibilityMeters", 10000)
            put("category", "Clear")
        }
        "storm_fog_alert_card" -> buildJsonObject {
            put("alertActive", false)
            put("conditionCode", 800)
            put("message", "No storm or fog warnings in effect")
        }
        else -> buildJsonObject {
            put("status", "mocked")
            put("message", "Sample widget data")
        }
    }
}
