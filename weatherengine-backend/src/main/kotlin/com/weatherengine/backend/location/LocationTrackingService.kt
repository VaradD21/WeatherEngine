package com.weatherengine.backend.location

import com.weatherengine.backend.weather.OpenMeteoForecastResponse
import com.weatherengine.backend.weather.WeatherBundleDto
import com.weatherengine.backend.weather.WeatherCacheService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.abs
import kotlin.math.round

@Service
class LocationTrackingService(
    private val locationHistoryRepository: UserLocationHistoryRepository,
    private val weatherCacheService: WeatherCacheService
) {

    private val logger = LoggerFactory.getLogger(LocationTrackingService::class.java)

    companion object {
        // Debounce window: If device reports within 45 minutes and moved < 250m, merge snapshot
        private const val DEBOUNCE_MINUTES = 45L
        private const val DISTANCE_THRESHOLD_DEGREES = 0.0025 // ~250-275 meters
    }

    @Transactional
    fun recordLocation(userId: UUID, request: RecordLocationRequest): LocationRecordResponse {
        val now = Instant.now()
        val zdt = ZonedDateTime.ofInstant(now, ZoneId.of("UTC"))
        val currentHour = zdt.hour
        val currentDayOfWeek = zdt.dayOfWeek.value // 1 (Mon) to 7 (Sun)

        // Compact coordinates to 4 decimal places (~11 meters precision)
        val compactLat = round(request.latitude * 10000.0) / 10000.0
        val compactLon = round(request.longitude * 10000.0) / 10000.0

        val lastRecord = locationHistoryRepository.findTopByUserIdOrderByRecordedAtDesc(userId)
        val isNewSlice: Boolean

        if (lastRecord != null &&
            abs(now.epochSecond - lastRecord.recordedAt.epochSecond) < (DEBOUNCE_MINUTES * 60) &&
            abs(lastRecord.latitude - compactLat) < DISTANCE_THRESHOLD_DEGREES &&
            abs(lastRecord.longitude - compactLon) < DISTANCE_THRESHOLD_DEGREES
        ) {
            // Heartbeat / stationary debounce: Update timestamp without inserting redundant row
            lastRecord.recordedAt = now
            lastRecord.accuracyMeters = request.accuracyMeters
            locationHistoryRepository.save(lastRecord)
            isNewSlice = false
        } else {
            // New hourly slice or user moved to a new location
            val newRecord = UserLocationHistory(
                userId = userId,
                latitude = compactLat,
                longitude = compactLon,
                accuracyMeters = request.accuracyMeters,
                hourOfDay = currentHour,
                dayOfWeek = currentDayOfWeek,
                recordedAt = now
            )
            locationHistoryRepository.save(newRecord)
            isNewSlice = true
        }

        // Predict upcoming weather hazards for expected routine
        val predictiveAlert = evaluatePredictiveAlert(userId, compactLat, compactLon, currentHour, currentDayOfWeek)

        return LocationRecordResponse(
            status = "OK",
            isNewSlice = isNewSlice,
            latitude = compactLat,
            longitude = compactLon,
            recordedAt = now.toString(),
            predictedAlert = predictiveAlert
        )
    }

    @Transactional(readOnly = true)
    fun getRecentHistory(userId: UUID, limit: Int = 24): List<LocationHistoryItemDto> {
        val boundedLimit = limit.coerceIn(1, 50)
        return locationHistoryRepository.findTop50ByUserIdOrderByRecordedAtDesc(userId)
            .take(boundedLimit)
            .map {
                LocationHistoryItemDto(
                    id = it.id ?: 0L,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    hourOfDay = it.hourOfDay,
                    dayOfWeek = it.dayOfWeek,
                    recordedAt = it.recordedAt.toString()
                )
            }
    }

    @Transactional(readOnly = true)
    fun getUpcomingAlerts(userId: UUID): List<PredictiveWeatherAlertDto> {
        val now = Instant.now()
        val zdt = ZonedDateTime.ofInstant(now, ZoneId.of("UTC"))
        val currentHour = zdt.hour
        val currentDay = zdt.dayOfWeek.value

        val latest = locationHistoryRepository.findTopByUserIdOrderByRecordedAtDesc(userId)
        val fallbackLat = latest?.latitude ?: 28.6139
        val fallbackLon = latest?.longitude ?: 77.2090

        val alerts = mutableListOf<PredictiveWeatherAlertDto>()

        // Lookahead next 1, 2, 3, and 4 hours
        for (offset in 1..4) {
            val targetHour = (currentHour + offset) % 24
            val targetDay = if (targetHour < currentHour) (currentDay % 7) + 1 else currentDay
            val alert = evaluatePredictiveAlert(userId, fallbackLat, fallbackLon, targetHour, targetDay)
            if (alert != null) {
                alerts.add(alert)
            }
        }
        return alerts
    }

    private fun evaluatePredictiveAlert(
        userId: UUID,
        fallbackLat: Double,
        fallbackLon: Double,
        targetHour: Int,
        targetDay: Int
    ): PredictiveWeatherAlertDto? {
        val historyForSlot = locationHistoryRepository.findByUserIdAndDayOfWeekAndHourOfDay(
            userId = userId,
            dayOfWeek = targetDay,
            hourOfDay = targetHour
        )

        val targetLat: Double
        val targetLon: Double

        if (historyForSlot.isNotEmpty()) {
            // Pick most frequent location (mode cluster centroid)
            val mostFrequent = historyForSlot.groupBy { "${it.latitude},${it.longitude}" }
                .maxByOrNull { it.value.size }
                ?.value?.first()
            targetLat = mostFrequent?.latitude ?: fallbackLat
            targetLon = mostFrequent?.longitude ?: fallbackLon
        } else {
            targetLat = fallbackLat
            targetLon = fallbackLon
        }

        return try {
            val bundle = weatherCacheService.getCachedWeatherBundle(targetLat, targetLon)
            inspectForecastForHazards(bundle, targetHour, targetLat, targetLon)
        } catch (ex: Exception) {
            logger.warn("Could not fetch forecast for predictive location ($targetLat, $targetLon): ${ex.message}")
            null
        }
    }

    private fun inspectForecastForHazards(
        bundle: WeatherBundleDto,
        targetHour: Int,
        lat: Double,
        lon: Double
    ): PredictiveWeatherAlertDto? {
        val hourly = bundle.forecast.hourly ?: return null
        val timeStrings = hourly.time

        // Find index matching target hour
        val hourSuffix = String.format("T%02d:00", targetHour)
        val index = timeStrings.indexOfFirst { it.endsWith(hourSuffix) }
        if (index == -1) return null

        val temp = hourly.temperature2m.getOrNull(index) ?: 25.0
        val precipProb = hourly.precipitationProbability.getOrNull(index) ?: 0
        val precipMm = hourly.precipitation.getOrNull(index) ?: 0.0
        val weatherCode = hourly.weatherCode.getOrNull(index) ?: 0
        val aqi = bundle.airQuality?.current?.usAqi ?: 50

        val targetTimeFormatted = formatHour(targetHour)

        // 1. Rain hazard check
        if (precipProb >= 60 || precipMm >= 2.0 || weatherCode in listOf(61, 63, 65, 80, 81, 82)) {
            return PredictiveWeatherAlertDto(
                targetHour = targetHour,
                targetTimeFormatted = targetTimeFormatted,
                predictedLatitude = lat,
                predictedLongitude = lon,
                alertType = "RAIN",
                headline = "🌧️ Rain Forecast for $targetTimeFormatted",
                message = "Rain (${precipProb}% chance) is forecast where you are expected to be around $targetTimeFormatted. Carry an umbrella!",
                severity = if (precipMm >= 5.0) "CRITICAL" else "WARNING"
            )
        }

        // 2. Severe Storm / Fog hazard check
        if (weatherCode in listOf(95, 96, 99)) {
            return PredictiveWeatherAlertDto(
                targetHour = targetHour,
                targetTimeFormatted = targetTimeFormatted,
                predictedLatitude = lat,
                predictedLongitude = lon,
                alertType = "STORM",
                headline = "⛈️ Thunderstorm Alert around $targetTimeFormatted",
                message = "Thunderstorm activity detected on your predicted route for $targetTimeFormatted. Plan your commute carefully.",
                severity = "CRITICAL"
            )
        }

        if (weatherCode in listOf(45, 48)) {
            return PredictiveWeatherAlertDto(
                targetHour = targetHour,
                targetTimeFormatted = targetTimeFormatted,
                predictedLatitude = lat,
                predictedLongitude = lon,
                alertType = "STORM_FOG",
                headline = "🌫️ Dense Fog Alert around $targetTimeFormatted",
                message = "Dense fog expected with poor visibility for your typical $targetTimeFormatted transit.",
                severity = "WARNING"
            )
        }

        // 3. Extreme Heat check
        if (temp >= 38.0) {
            return PredictiveWeatherAlertDto(
                targetHour = targetHour,
                targetTimeFormatted = targetTimeFormatted,
                predictedLatitude = lat,
                predictedLongitude = lon,
                alertType = "SEVERE_HEAT",
                headline = "☀️ Extreme Heat Advisory for $targetTimeFormatted",
                message = "Temperatures will reach ${temp.toInt()}°C near your anticipated location. Stay hydrated and avoid direct sun.",
                severity = "WARNING"
            )
        }

        // 4. Frost / Freezing check
        if (temp <= 0.0) {
            return PredictiveWeatherAlertDto(
                targetHour = targetHour,
                targetTimeFormatted = targetTimeFormatted,
                predictedLatitude = lat,
                predictedLongitude = lon,
                alertType = "FROST",
                headline = "❄️ Freezing Frost Alert for $targetTimeFormatted",
                message = "Freezing temperatures (${temp.toInt()}°C) expected at $targetTimeFormatted. Watch for icy surfaces and dress warmly.",
                severity = "WARNING"
            )
        }

        // 5. High AQI check
        if (aqi >= 150) {
            return PredictiveWeatherAlertDto(
                targetHour = targetHour,
                targetTimeFormatted = targetTimeFormatted,
                predictedLatitude = lat,
                predictedLongitude = lon,
                alertType = "POOR_AQI",
                headline = "😷 Air Quality Warning for $targetTimeFormatted",
                message = "AQI of $aqi (Unhealthy) is forecast where you are typically located around $targetTimeFormatted. Consider a mask or staying indoors.",
                severity = "WARNING"
            )
        }

        return null
    }

    private fun formatHour(hour: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        return "$displayHour:00 $period"
    }
}
