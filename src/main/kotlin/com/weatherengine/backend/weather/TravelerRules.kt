package com.weatherengine.backend.weather

import com.weatherengine.backend.location.Location
import java.time.LocalDateTime
import kotlin.math.roundToInt

data class DestinationWeatherSummary(
    val id: String,
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val tempCelsius: Double,
    val condition: String,
    val weatherCode: Int,
    val packingTip: String
)

object TravelerRules {

    fun computeFlightAlertCard(
        current: OpenMeteoCurrentWeather?,
        hourly: OpenMeteoHourlyForecast?,
        nowLocal: LocalDateTime
    ): Map<String, Any?> {
        val next12Hours = hourly?.time?.take(12)?.indices?.toList() ?: emptyList()

        var hasSevereRisk = false
        var alertSeverity = "Normal"
        val reasons = mutableListOf<String>()

        val currGusts = current?.windGusts10m ?: 0.0
        val currCode = current?.weatherCode ?: 0

        val isStormNow = currCode in setOf(95, 96, 99)
        val isFogNow = currCode in setOf(45, 48)
        val isSnowNow = currCode in setOf(71, 73, 75, 77, 85, 86)

        if (isStormNow) {
            hasSevereRisk = true
            alertSeverity = "Severe"
            reasons.add("Thunderstorms active at departure/arrival zone (WMO $currCode)")
        }

        if (currGusts >= TravelerConstants.FLIGHT_SEVERE_WIND_GUST_KMH) {
            hasSevereRisk = true
            if (alertSeverity != "Severe") alertSeverity = "Warning"
            reasons.add("Strong crosswinds/gusts up to ${currGusts.roundToInt()} km/h")
        }

        if (isFogNow) {
            hasSevereRisk = true
            if (alertSeverity != "Severe") alertSeverity = "Caution"
            reasons.add("Low visibility fog causing potential taxiway/approach delays")
        }

        if (isSnowNow) {
            hasSevereRisk = true
            if (alertSeverity != "Severe") alertSeverity = "Warning"
            reasons.add("Snow / de-icing conditions in effect")
        }

        for (i in next12Hours) {
            val code = hourly?.weatherCode?.getOrNull(i) ?: 0
            val gust = hourly?.windGusts10m?.getOrNull(i) ?: 0.0
            val vis = hourly?.visibility?.getOrNull(i) ?: 10000.0
            val precipProb = hourly?.precipitationProbability?.getOrNull(i) ?: 0

            if (code in setOf(95, 96, 99) && reasons.none { it.contains("Thunderstorm") }) {
                hasSevereRisk = true
                alertSeverity = "Severe"
                reasons.add("Thunderstorms active in upcoming flight window")
            }
            if (gust >= TravelerConstants.FLIGHT_SEVERE_WIND_GUST_KMH && reasons.none { it.contains("crosswinds") }) {
                hasSevereRisk = true
                if (alertSeverity == "Normal") alertSeverity = "Warning"
                reasons.add("Severe gusts exceeding 55 km/h expected")
            }
            if (vis <= TravelerConstants.FLIGHT_DENSE_FOG_VISIBILITY_METERS && reasons.none { it.contains("visibility") }) {
                hasSevereRisk = true
                if (alertSeverity == "Normal") alertSeverity = "Caution"
                reasons.add("Low visibility dense fog (<1km visibility) expected")
            }
            if (precipProb >= TravelerConstants.FLIGHT_HEAVY_PRECIP_PROBABILITY && reasons.none { it.contains("Heavy rain") }) {
                if (alertSeverity == "Normal") alertSeverity = "Caution"
                reasons.add("Heavy precipitation ($precipProb% chance) in route corridor")
            }
        }

        val message = when (alertSeverity) {
            "Severe" -> "Severe flight disruption risk: Thunderstorms or extreme conditions detected"
            "Warning" -> "Moderate flight delay risk: High winds or winter weather advisory"
            "Caution" -> "Minor flight delay risk: Reduced visibility or showers expected"
            else -> "Smooth flying conditions expected. No major weather-related flight delays forecast."
        }

        val delayRisk = when (alertSeverity) {
            "Severe" -> "High"
            "Warning", "Caution" -> "Moderate"
            else -> "Minimal"
        }

        val primaryReason = if (reasons.isNotEmpty()) reasons.first() else "Calm skies and favorable route visibility"

        return mapOf(
            "status" to "ok",
            "hasDisruptionRisk" to hasSevereRisk,
            "severity" to alertSeverity,
            "delayRisk" to delayRisk,
            "reason" to primaryReason,
            "message" to message,
            "reasons" to reasons,
            "flightStatus" to if (hasSevereRisk) "Delays Likely" else "On Schedule"
        )
    }

    fun computePackingSuggestionCard(
        current: OpenMeteoCurrentWeather?,
        daily: OpenMeteoDailyForecast?,
        hourly: OpenMeteoHourlyForecast?,
        locationName: String = "your destination"
    ): Map<String, Any?> {
        val tempNow = current?.apparentTemperature ?: current?.temperature2m ?: 22.0
        val tempMax = daily?.temperature2mMax?.firstOrNull() ?: tempNow
        val tempMin = daily?.temperature2mMin?.firstOrNull() ?: (tempNow - 5.0)

        val rainChanceMax = daily?.precipitationProbabilityMax?.firstOrNull()
            ?: hourly?.precipitationProbability?.filterNotNull()?.take(24)?.maxOrNull()
            ?: 0

        val uvMax = daily?.uvIndexMax?.firstOrNull()
            ?: hourly?.uvIndex?.filterNotNull()?.take(24)?.maxOrNull()
            ?: 3.0

        val items = mutableListOf<String>()
        val tips = mutableListOf<String>()

        if (rainChanceMax >= TravelerConstants.PACKING_RAIN_CHANCE_MIN) {
            items.add("Raincoat or compact umbrella")
            tips.add("Carry a raincoat or umbrella ($rainChanceMax% chance of rain in $locationName)")
        }

        when {
            tempMin <= TravelerConstants.TEMP_FREEZING_MAX -> {
                items.add("Heavy thermal coat & gloves")
                items.add("Warm beanie or woolen scarf")
                tips.add("Freezing lows down to ${tempMin.roundToInt()}°C — pack heavy thermal layers")
            }
            tempMin <= TravelerConstants.TEMP_COOL_MAX -> {
                items.add("Warm jacket / sweater")
                items.add("Layered clothing")
                tips.add("Chilly evenings (~${tempMin.roundToInt()}°C) — pack a versatile jacket")
            }
            tempMax >= TravelerConstants.TEMP_HOT_MIN -> {
                items.add("Breathable cotton or linen clothes")
                items.add("Hydration bottle")
                tips.add("Hot days reaching ${tempMax.roundToInt()}°C — pack lightweight clothing")
            }
            else -> {
                items.add("Light jacket or cardigan")
                items.add("Comfortable walking shoes")
                tips.add("Pleasant weather (${tempMin.roundToInt()}°C - ${tempMax.roundToInt()}°C)")
            }
        }

        if (uvMax >= TravelerConstants.PACKING_HIGH_UV_MIN) {
            items.add("Sunglasses & SPF 50+ sunscreen")
            tips.add("High UV index (${((uvMax * 10).roundToInt()) / 10.0}) — pack sunglasses & sun protection")
        }

        val headline = if (rainChanceMax >= 50) {
            "Carry a raincoat in $locationName"
        } else if (tempMin <= TravelerConstants.TEMP_COOL_MAX) {
            "Pack warm layers for $locationName"
        } else {
            "Pack light & comfortable for $locationName"
        }

        val tempRange = "${tempMin.roundToInt()}°C - ${tempMax.roundToInt()}°C"

        return mapOf(
            "status" to "ok",
            "destination" to locationName,
            "headline" to headline,
            "items" to items,
            "tips" to tips,
            "tempRange" to tempRange,
            "rainChancePercent" to rainChanceMax,
            "uvIndexMax" to uvMax,
            "message" to "$headline • $tempRange"
        )
    }

    fun computeSavedDestinationsCard(
        savedLocations: List<Location> = emptyList()
    ): Map<String, Any?> {
        if (savedLocations.size < 2) {
            return mapOf(
                "status" to "empty",
                "destinations" to emptyList<Map<String, Any?>>(),
                "message" to "Add saved destinations in Locations tab"
            )
        }

        val destinations = savedLocations.map { loc ->
            mapOf(
                "id" to (loc.id?.toString() ?: "loc_${loc.label}"),
                "name" to (loc.label ?: "Saved Location"),
                "lat" to loc.latitude.toDouble(),
                "lon" to loc.longitude.toDouble(),
                "isPrimary" to loc.isPrimary
            )
        }

        return mapOf(
            "status" to "ok",
            "destinations" to destinations,
            "message" to "${destinations.size} saved destinations"
        )
    }

    fun computeSavedDestinationsCard(
        currentLocationLat: Double,
        currentLocationLon: Double
    ): Map<String, Any?> = computeSavedDestinationsCard(emptyList())
}
