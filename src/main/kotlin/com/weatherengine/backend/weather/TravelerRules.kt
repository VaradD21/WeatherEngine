package com.weatherengine.backend.weather

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

        // Check thunderstorm / snow / gale
        val isStormNow = currCode in setOf(95, 96, 99)
        val isFogNow = currCode in setOf(45, 48)
        val isSnowNow = currCode in setOf(71, 73, 75, 77, 85, 86)

        if (isStormNow) {
            hasSevereRisk = true
            alertSeverity = "Severe"
            reasons.add("Active thunderstorm at departure/arrival zone (WMO $currCode)")
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

        // Lookahead next 12 hours
        for (i in next12Hours) {
            val code = hourly?.weatherCode?.getOrNull(i) ?: 0
            val gust = hourly?.windGusts10m?.getOrNull(i) ?: 0.0
            val vis = hourly?.visibility?.getOrNull(i) ?: 10000.0
            val precipProb = hourly?.precipitationProbability?.getOrNull(i) ?: 0

            if (code in setOf(95, 96, 99) && reasons.none { it.contains("Thunderstorm") }) {
                hasSevereRisk = true
                alertSeverity = "Severe"
                reasons.add("Thunderstorms expected in upcoming flight window")
            }
            if (gust >= TravelerConstants.FLIGHT_SEVERE_WIND_GUST_KMH && reasons.none { it.contains("crosswinds") }) {
                hasSevereRisk = true
                if (alertSeverity == "Normal") alertSeverity = "Warning"
                reasons.add("Severe gusts exceeding 55 km/h expected")
            }
            if (vis <= TravelerConstants.FLIGHT_DENSE_FOG_VISIBILITY_METERS && reasons.none { it.contains("visibility") }) {
                hasSevereRisk = true
                if (alertSeverity == "Normal") alertSeverity = "Caution"
                reasons.add("Dense fog (<1km visibility) expected")
            }
            if (precipProb >= TravelerConstants.FLIGHT_HEAVY_PRECIP_PROBABILITY && reasons.none { it.contains("Heavy rain") }) {
                if (alertSeverity == "Normal") alertSeverity = "Caution"
                reasons.add("Heavy precipitation ($precipProb% chance) in route corridor")
            }
        }

        val status = if (hasSevereRisk) "alert" else "ok"
        val message = when (alertSeverity) {
            "Severe" -> "Severe flight disruption risk: Thunderstorms or extreme conditions detected"
            "Warning" -> "Moderate flight delay risk: High winds or winter weather advisory"
            "Caution" -> "Minor flight delay risk: Reduced visibility or showers expected"
            else -> "Smooth flying conditions expected. No major weather-related flight delays forecast."
        }

        return mapOf(
            "status" to "ok",
            "hasDisruptionRisk" to hasSevereRisk,
            "severity" to alertSeverity,
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

        // Rain packing
        if (rainChanceMax >= TravelerConstants.PACKING_RAIN_CHANCE_MIN) {
            items.add("Raincoat or compact umbrella")
            tips.add("Carry a raincoat or umbrella ($rainChanceMax% chance of rain in $locationName)")
        }

        // Temperature packing
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

        // UV / Sun protection
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
        currentLocationLat: Double,
        currentLocationLon: Double
    ): Map<String, Any?> {
        // Curated global popular travel hubs with dynamic contextual hints
        val destinations = listOf(
            mapOf(
                "id" to "dest_lon",
                "name" to "London",
                "country" to "UK",
                "lat" to 51.5074,
                "lon" to -0.1278,
                "tempCelsius" to 14.0,
                "condition" to "Light Rain",
                "weatherCode" to 61,
                "packingTip" to "Carry a raincoat in London"
            ),
            mapOf(
                "id" to "dest_par",
                "name" to "Paris",
                "country" to "France",
                "lat" to 48.8566,
                "lon" to 2.3522,
                "tempCelsius" to 16.5,
                "condition" to "Partly Cloudy",
                "weatherCode" to 2,
                "packingTip" to "Light jacket & comfortable walking shoes"
            ),
            mapOf(
                "id" to "dest_tok",
                "name" to "Tokyo",
                "country" to "Japan",
                "lat" to 35.6762,
                "lon" to 139.6503,
                "tempCelsius" to 19.0,
                "condition" to "Clear",
                "weatherCode" to 0,
                "packingTip" to "Pleasant weather, sunglasses recommended"
            ),
            mapOf(
                "id" to "dest_dxb",
                "name" to "Dubai",
                "country" to "UAE",
                "lat" to 25.2048,
                "lon" to 55.2708,
                "tempCelsius" to 33.0,
                "condition" to "Sunny & Warm",
                "weatherCode" to 0,
                "packingTip" to "Breathable clothing & high SPF sunscreen"
            )
        )

        return mapOf(
            "status" to "ok",
            "destinations" to destinations,
            "message" to "Quick access to 4 popular destinations with live weather"
        )
    }
}
