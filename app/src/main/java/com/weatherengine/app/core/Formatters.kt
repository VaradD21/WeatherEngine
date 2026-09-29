package com.weatherengine.app.core

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.round

data class AqiDisplay(
    val label: String,
    val level: Int
)

object Formatters {

    fun aqiInfo(aqi: Int?): AqiDisplay = when (aqi) {
        1 -> AqiDisplay("Good", 1)
        2 -> AqiDisplay("Fair", 2)
        3 -> AqiDisplay("Moderate", 3)
        4 -> AqiDisplay("Poor", 4)
        5 -> AqiDisplay("Very Poor", 5)
        else -> AqiDisplay("Unknown", 0)
    }

    fun windMsToKmh(ms: Double?): Double? {
        if (ms == null || ms.isNaN() || ms.isInfinite()) return null
        return round(ms * 3.6 * 10.0) / 10.0
    }

    fun formatClock(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "—"
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            // Try parsing with offset / zone first
            val time = try {
                OffsetDateTime.parse(isoString).toLocalTime()
            } catch (_: Exception) {
                try {
                    Instant.parse(isoString).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                } catch (_: Exception) {
                    java.time.LocalDateTime.parse(isoString).toLocalTime()
                }
            }
            time.format(formatter)
        } catch (_: Exception) {
            "—"
        }
    }

    fun minutesAgo(timestampMs: Long, nowMs: Long): Long {
        val diffMs = nowMs - timestampMs
        if (diffMs <= 0) return 0L
        return diffMs / 60000L
    }

    fun widgetTitle(type: String?): String {
        if (type.isNullOrBlank()) return "Weather Card"
        return when (type) {
            "aqi_card" -> "Air Quality"
            "humidity_card" -> "Humidity"
            "uv_index_card" -> "UV Index"
            "sunrise_sunset_card" -> "Sunrise & Sunset"
            "wind_speed_card" -> "Wind Speed"
            "heat_alert_card" -> "Heat Alert"
            "traffic_card" -> "Traffic"
            "visibility_card" -> "Visibility"
            "storm_fog_alert_card" -> "Storm & Fog Alert"
            else -> {
                var cleaned = type.trim()
                if (cleaned.endsWith("_card")) {
                    cleaned = cleaned.substring(0, cleaned.length - 5)
                }
                cleaned.replace('_', ' ')
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    }
            }
        }
    }
}
