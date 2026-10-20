package com.weatherengine.backend.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

/** Widget codes accepted by the backend widget builder. */
private val KNOWN_WIDGET_CODES = setOf(
    "aqi_card", "humidity_card", "uv_index_card", "sunrise_sunset_card",
    "wind_speed_card", "heat_alert_card", "traffic_card", "visibility_card",
    "storm_fog_alert_card", "pollen_card", "best_running_hours_card",
    "wave_height_card", "sea_temp_card", "tide_card",
    "school_commute_card", "rain_alert_card", "severe_weather_card",
    "saved_destinations_card", "flight_alert_card", "packing_suggestion_card",
    "soil_moisture_card", "rainfall_prediction_card", "frost_alert_card",
    "planting_guidance_card", "extended_forecast_card", "rain_probability_card",
    "comfort_index_card"
)

/** Allowed values for temperatureUnit. */
private val ALLOWED_TEMP_UNITS = setOf("CELSIUS", "FAHRENHEIT")

/** Allowed values for defaultScreen. */
private val ALLOWED_SCREENS = setOf("home", "commute", "health", "travel", "garden", "beach", "event", "running")

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserPreferencesDto(
    @field:Size(max = 30, message = "widgetOrder may contain at most 30 items")
    val widgetOrder: List<String> = emptyList(),

    @field:Size(max = 30, message = "disabledWidgets may contain at most 30 items")
    val disabledWidgets: Set<String> = emptySet(),

    val defaultScreen: String = "home",
    val temperatureUnit: String = "CELSIUS",

    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "schoolStart must be in HH:mm 24-hour format")
    val schoolStart: String = "08:00",

    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "schoolEnd must be in HH:mm 24-hour format")
    val schoolEnd: String = "15:00",

    val updatedAt: Instant? = null
) {
    /**
     * Validates business-level constraints not expressible as annotations:
     * - each widgetOrder item is a known code and max 32 chars
     * - each disabledWidgets item is a known code and max 32 chars
     * - temperatureUnit is one of the allowed enum strings
     * - defaultScreen is one of the allowed screen names
     *
     * Called by the controller before persisting.
     * Throws [IllegalArgumentException] with a descriptive message on the first violation.
     */
    fun validateBusinessRules() {
        require(widgetOrder.size <= 30) { "widgetOrder may contain at most 30 items" }
        require(disabledWidgets.size <= 30) { "disabledWidgets may contain at most 30 items" }

        for (code in widgetOrder) {
            require(code.length <= 32) { "Widget code '$code' exceeds 32 characters" }
            require(code in KNOWN_WIDGET_CODES) { "Unknown widget code: '$code'" }
        }
        for (code in disabledWidgets) {
            require(code.length <= 32) { "Disabled widget code '$code' exceeds 32 characters" }
            require(code in KNOWN_WIDGET_CODES) { "Unknown disabled widget code: '$code'" }
        }
        require(temperatureUnit in ALLOWED_TEMP_UNITS) {
            "temperatureUnit must be one of $ALLOWED_TEMP_UNITS"
        }
        require(defaultScreen.lowercase() in ALLOWED_SCREENS) {
            "defaultScreen must be one of $ALLOWED_SCREENS"
        }
    }
}
