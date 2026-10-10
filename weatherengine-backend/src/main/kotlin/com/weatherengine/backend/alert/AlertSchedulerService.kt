package com.weatherengine.backend.alert

import com.weatherengine.backend.user.UserHealthProfile
import com.weatherengine.backend.user.UserHealthProfileRepository
import com.weatherengine.backend.weather.WeatherBundleDto
import com.weatherengine.backend.weather.WeatherCacheService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.Locale

@Service
class AlertSchedulerService(
    private val subscriptionRepository: AlertSubscriptionRepository,
    private val alertLogRepository: AlertLogRepository,
    private val weatherCacheService: WeatherCacheService,
    private val userHealthProfileRepository: UserHealthProfileRepository
) {
    private val logger = LoggerFactory.getLogger(AlertSchedulerService::class.java)

    companion object {
        private val DEDUP_WINDOW: Duration = Duration.ofHours(6)
    }

    @Scheduled(fixedDelayString = "\${app.alerts.poll-interval-ms:900000}", initialDelay = 60000)
    @Transactional
    fun evaluateAndSendAlerts() {
        val activeSubscriptions = subscriptionRepository.findByActiveTrue()
        if (activeSubscriptions.isEmpty()) return

        val cutoff = Instant.now().minus(DEDUP_WINDOW)

        for (sub in activeSubscriptions) {
            val location = sub.location
            val locationId = location.id ?: continue
            val lat = location.latitude.toDouble()
            val lon = location.longitude.toDouble()

            val latKey = String.format(Locale.US, "%.2f", lat)
            val lonKey = String.format(Locale.US, "%.2f", lon)

            val bundle = try {
                weatherCacheService.getCachedWeatherBundle(lat, lon)
            } catch (ex: Exception) {
                logger.warn("Skipping alert check for location {}: {}", locationId, ex.message)
                continue
            }

            val healthProfile = sub.user.id?.let { userHealthProfileRepository.findById(it).orElse(null) }
            val triggeredType = detectAlertCondition(bundle, healthProfile) ?: continue
            val recentLogs = alertLogRepository.findByLocationIdAndAlertTypeAndSentAtAfter(
                locationId = locationId,
                alertType = triggeredType,
                after = cutoff
            )

            if (recentLogs.isEmpty()) {
                logger.info(
                    "Dispatching {} alert for location ({}, {}) to subscription {}",
                    triggeredType,
                    latKey,
                    lonKey,
                    sub.id
                )
                alertLogRepository.save(
                    AlertLog(
                        location = location,
                        alertType = triggeredType,
                        sentAt = Instant.now()
                    )
                )
            }
        }
    }

    internal fun detectAlertCondition(
        bundle: WeatherBundleDto,
        profile: UserHealthProfile? = null
    ): String? {
        val current = bundle.forecast.current
        val hourly = bundle.forecast.hourly
        val daily = bundle.forecast.daily

        val weatherCode = current?.weatherCode ?: 0
        if (weatherCode in setOf(95, 96, 99)) {
            return "STORM_ALERT"
        }

        val apparentTemp = current?.apparentTemperature ?: current?.temperature2m ?: 25.0
        if (apparentTemp >= 38.0) {
            return "EXTREME_HEAT"
        }

        val aqi = bundle.airQuality?.current?.usAqi ?: 0
        val uv = hourly?.uvIndex?.firstOrNull { it != null } ?: daily?.uvIndexMax?.firstOrNull() ?: 0.0
        val humidity = current?.relativeHumidity2m ?: 50

        if (profile != null && profile.alertsEnabled) {
            if (aqi >= profile.aqiThreshold) {
                return if (profile.hasAsthma) "ASTHMA_AQI_ALERT" else "HIGH_AQI"
            }
            if (uv >= profile.uvThreshold) {
                return if (profile.hasSkinSensitivity) "SKIN_UV_ALERT" else "HIGH_UV_ALERT"
            }
            if (humidity >= profile.humidityThreshold) {
                return "HIGH_HUMIDITY_ALERT"
            }
            if (profile.hasAllergies) {
                val pollen = listOfNotNull(
                    bundle.airQuality?.current?.grassPollen,
                    bundle.airQuality?.current?.birchPollen,
                    bundle.airQuality?.current?.ragweedPollen
                ).maxOrNull() ?: 0.0
                if (pollen >= 50.0) {
                    return "POLLEN_ALLERGY_ALERT"
                }
            }
        }

        if (aqi >= 151) {
            return "HIGH_AQI"
        }

        return null
    }

    internal fun detectAlertCondition(bundle: WeatherBundleDto): String? =
        detectAlertCondition(bundle, null)
}
