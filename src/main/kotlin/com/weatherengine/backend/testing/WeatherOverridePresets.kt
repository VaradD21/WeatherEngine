package com.weatherengine.backend.testing

import java.util.UUID

object WeatherOverridePresets {

    fun createPreset(
        presetKey: String,
        lat: Double,
        lon: Double,
        radiusKm: Double = 10.0,
        targetUserId: UUID? = null,
        label: String? = null
    ): WeatherOverrideDto {
        val baseLabel = label ?: presetKey.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

        return when (presetKey.uppercase()) {
            "HAZARDOUS_AQI_1000" -> WeatherOverrideDto(
                label = "$baseLabel (AQI 1000 Disaster)",
                latitude = lat,
                longitude = lon,
                radiusKm = radiusKm,
                targetUserId = targetUserId,
                usAqi = 1000,
                pm25 = 480.0,
                grassPollen = 85.0,
                birchPollen = 110.0,
                alderPollen = 60.0,
                ragweedPollen = 40.0,
                weatherCode = 45, // Fog/Smog
                visibility = 600.0,
                uvIndex = 2.0
            )

            "SEVERE_THUNDERSTORM" -> WeatherOverrideDto(
                label = "$baseLabel (WMO 95 Squall)",
                latitude = lat,
                longitude = lon,
                radiusKm = radiusKm,
                targetUserId = targetUserId,
                weatherCode = 95,
                temperature2m = 22.0,
                apparentTemperature = 21.0,
                relativeHumidity2m = 95,
                precipitation = 35.0,
                precipitationProbability = 95,
                windSpeed10m = 75.0,
                windGusts10m = 98.0,
                visibility = 1200.0,
                uvIndex = 1.0,
                waveHeight = 4.8
            )

            "HEAT_WAVE_45C" -> WeatherOverrideDto(
                label = "$baseLabel (Extreme Heat 45°C)",
                latitude = lat,
                longitude = lon,
                radiusKm = radiusKm,
                targetUserId = targetUserId,
                temperature2m = 45.0,
                apparentTemperature = 49.5,
                relativeHumidity2m = 40,
                weatherCode = 0,
                uvIndex = 12.0,
                precipitation = 0.0,
                precipitationProbability = 0,
                windSpeed10m = 18.0
            )

            "DENSE_FOG" -> WeatherOverrideDto(
                label = "$baseLabel (Dense Fog 150m)",
                latitude = lat,
                longitude = lon,
                radiusKm = radiusKm,
                targetUserId = targetUserId,
                weatherCode = 45,
                temperature2m = 12.0,
                apparentTemperature = 11.0,
                relativeHumidity2m = 98,
                visibility = 150.0,
                windSpeed10m = 4.0,
                precipitationProbability = 15
            )

            "MARINE_GALE" -> WeatherOverrideDto(
                label = "$baseLabel (Gale & 6.5m Waves)",
                latitude = lat,
                longitude = lon,
                radiusKm = radiusKm,
                targetUserId = targetUserId,
                waveHeight = 6.5,
                seaSurfaceTemperature = 17.5,
                windSpeed10m = 65.0,
                windGusts10m = 85.0,
                weatherCode = 65 // Heavy Rain
            )

            "POLLEN_SPIKE" -> WeatherOverrideDto(
                label = "$baseLabel (High Allergy Pollen)",
                latitude = lat,
                longitude = lon,
                radiusKm = radiusKm,
                targetUserId = targetUserId,
                grassPollen = 140.0,
                birchPollen = 180.0,
                alderPollen = 95.0,
                ragweedPollen = 120.0,
                usAqi = 160,
                pm25 = 55.0,
                weatherCode = 1,
                temperature2m = 24.0,
                uvIndex = 7.0
            )

            "BLIZZARD_MINUS_10" -> WeatherOverrideDto(
                label = "$baseLabel (Blizzard -10°C)",
                latitude = lat,
                longitude = lon,
                radiusKm = radiusKm,
                targetUserId = targetUserId,
                temperature2m = -10.0,
                apparentTemperature = -18.0,
                relativeHumidity2m = 88,
                weatherCode = 75, // Heavy Snow
                windSpeed10m = 55.0,
                windGusts10m = 75.0,
                precipitation = 18.0,
                precipitationProbability = 90,
                visibility = 400.0
            )

            else -> WeatherOverrideDto(
                label = "$baseLabel (Standard Test)",
                latitude = lat,
                longitude = lon,
                radiusKm = radiusKm,
                targetUserId = targetUserId,
                usAqi = 1000,
                pm25 = 450.0
            )
        }
    }
}
