package com.weatherengine.backend.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserPreferencesDto(
    val widgetOrder: List<String> = emptyList(),
    val disabledWidgets: Set<String> = emptySet(),
    val defaultScreen: String = "home",
    val temperatureUnit: String = "CELSIUS",
    val schoolStart: String = "08:00",
    val schoolEnd: String = "15:00",
    val updatedAt: Instant? = null
)
