package com.weatherengine.app.core.forecast

import com.weatherengine.app.core.Formatters
import com.weatherengine.app.data.openmeteo.OpenMeteoAirQualityResponse
import com.weatherengine.app.data.openmeteo.OpenMeteoCurrentAirQuality
import com.weatherengine.app.data.openmeteo.OpenMeteoForecastResponse
import com.weatherengine.app.persona.HourlyRowUi
import com.weatherengine.app.persona.PersonaConstants
import com.weatherengine.app.persona.PersonaRules
import com.weatherengine.app.persona.PollenInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object ForecastMapper {

    fun formatTemp(celsius: Double?): String {
        return if (celsius != null && !celsius.isNaN() && !celsius.isInfinite() &&
            celsius in PersonaConstants.MIN_TEMP_C..PersonaConstants.MAX_TEMP_C) {
            "${celsius.roundToInt()}°"
        } else {
            "—"
        }
    }

    fun dayLabel(dateIso: String, index: Int, @Suppress("UNUSED_PARAMETER") today: LocalDate = LocalDate.now()): String {
        if (index == 0) return "Today"
        if (index == 1) return "Tomorrow"
        return try {
            val date = LocalDate.parse(dateIso)
            date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
        } catch (_: Exception) {
            "Day ${index + 1}"
        }
    }

    fun formatUpdatedAt(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diffMs = nowMs - timestampMs
        if (diffMs < 60_000L) return "Updated just now"
        val minutes = diffMs / 60_000L
        if (minutes < 60) return "Updated $minutes min ago"
        val hours = minutes / 60
        return "Updated ${hours}h ago"
    }

    fun calculateRangeFractions(
        minTemps: List<Double>,
        maxTemps: List<Double>
    ): List<Pair<Float, Float>> {
        if (minTemps.isEmpty() || maxTemps.isEmpty()) return emptyList()
        val globalMin = minTemps.minOrNull() ?: 0.0
        val globalMax = maxTemps.maxOrNull() ?: 1.0
        val span = (globalMax - globalMin).coerceAtLeast(1.0)

        val size = minOf(minTemps.size, maxTemps.size)
        return (0 until size).map { i ->
            val dayMin = minTemps[i]
            val dayMax = maxTemps[i]
            val start = ((dayMin - globalMin) / span).toFloat().coerceIn(0f, 1f)
            val end = ((dayMax - globalMin) / span).toFloat().coerceIn(0f, 1f)
            Pair(start, maxOf(start, end))
        }
    }

    fun mapToForecastUi(
        forecastResponse: OpenMeteoForecastResponse,
        airQualityResponse: OpenMeteoAirQualityResponse? = null,
        locationLabel: String,
        timestampMs: Long = System.currentTimeMillis()
    ): ForecastUi {
        val current = forecastResponse.current
        val isDay = current?.isDay == 1
        val weatherInfo = WmoWeatherCode.info(current?.weatherCode, isDay = isDay)

        // Sanitize current metrics
        val currentTemp = current?.temperature2m?.takeIf { it in PersonaConstants.MIN_TEMP_C..PersonaConstants.MAX_TEMP_C }
        val currentApparent = current?.apparentTemperature?.takeIf { it in PersonaConstants.MIN_TEMP_C..PersonaConstants.MAX_TEMP_C }
        val currentHumidity = current?.relativeHumidity2m?.takeIf { it in PersonaConstants.MIN_HUMIDITY..PersonaConstants.MAX_HUMIDITY }
        val currentWind = current?.windSpeed10m?.takeIf { it >= 0.0 }
        val currentGusts = current?.windGusts10m?.takeIf { it >= 0.0 }
        val currentAqi = airQualityResponse?.current?.usAqi?.takeIf { it >= 0 }

        val allHourly = buildHourlyList(forecastResponse, airQualityResponse)
        val hourly24 = PersonaRules.hourlyWindow(allHourly, current?.time, 24)

        val firstDailySunrise = forecastResponse.daily?.sunrise?.firstOrNull()
        val firstDailySunset = forecastResponse.daily?.sunset?.firstOrNull()
        val daylight = PersonaRules.daylightInfo(current?.time, firstDailySunrise, firstDailySunset)

        val bestRunning = PersonaRules.bestRunningWindow(hourly24)
        val next24MaxApparent = hourly24.mapNotNull { it.apparentTempRaw }.maxOrNull()
        val heat = PersonaRules.heatStatus(next24MaxApparent)
        val stormFog = PersonaRules.stormFogStatus(hourly24, current?.time)
        val commute = PersonaRules.commuteRain(hourly24)
        val pollen = mapPollenInfo(airQualityResponse?.current)

        val currentUv = hourly24.firstOrNull()?.uvIndex
        val currentVisibility = hourly24.firstOrNull()?.visibilityM

        val currentWeatherUi = CurrentWeatherUi(
            temperatureFormatted = formatTemp(currentTemp),
            apparentTemperatureFormatted = currentApparent?.let { "Feels like ${formatTemp(it)}" } ?: "",
            condition = weatherInfo.description,
            iconKey = weatherInfo.iconKey,
            humidityFormatted = currentHumidity?.let { "$it%" } ?: "—",
            humidityPercent = currentHumidity,
            windFormatted = currentWind?.let { "$it km/h" } ?: "—",
            windSpeedKmh = currentWind,
            windGustsFormatted = currentGusts?.let { "Gusts: $it km/h" } ?: "",
            windGustsKmh = currentGusts,
            precipitationFormatted = current?.precipitation?.takeIf { it >= 0.0 }?.let { "$it mm" } ?: "0.0 mm",
            visibilityMeters = currentVisibility,
            uvIndex = currentUv,
            aqi = currentAqi,
            aqiCategory = PersonaRules.usAqiCategory(currentAqi),
            pm25 = airQualityResponse?.current?.pm25?.takeIf { it >= 0.0 },
            isDay = isDay
        )

        val daysList = buildDaysList(forecastResponse.daily)
        val uvMaxToday = forecastResponse.daily?.uvIndexMax?.firstOrNull()

        return ForecastUi(
            locationLabel = locationLabel,
            updatedAtLabel = formatUpdatedAt(timestampMs),
            timestampMs = timestampMs,
            current = currentWeatherUi,
            hourly = hourly24,
            days = daysList,
            daylight = daylight,
            bestRunning = bestRunning,
            heat = heat,
            stormFog = stormFog,
            commute = commute,
            pollen = pollen,
            uvMaxToday = uvMaxToday,
            attribution = "Weather data by Open-Meteo.com",
            attributionUrl = "https://open-meteo.com/",
            isCached = false
        )
    }

    private fun buildHourlyList(
        forecast: OpenMeteoForecastResponse,
        airQuality: OpenMeteoAirQualityResponse?
    ): List<HourlyRowUi> {
        val hourly = forecast.hourly ?: return emptyList()
        val times = hourly.time
        if (times.isEmpty()) return emptyList()

        // Zip using shortest length
        val length = listOf(
            times.size,
            hourly.temperature2m.size,
            hourly.apparentTemperature.size,
            hourly.relativeHumidity2m.size,
            hourly.precipitationProbability.size,
            hourly.weatherCode.size,
            hourly.windSpeed10m.size,
            hourly.windGusts10m.size,
            hourly.visibility.size,
            hourly.uvIndex.size,
            hourly.isDay.size
        ).minOrNull() ?: 0

        if (length == 0) return emptyList()

        val aqiMap = mutableMapOf<String, Int>()
        airQuality?.hourly?.let { hAqi ->
            val aqiLen = minOf(hAqi.time.size, hAqi.usAqi.size)
            for (i in 0 until aqiLen) {
                hAqi.usAqi.getOrNull(i)?.let { aqiVal ->
                    aqiMap[hAqi.time[i]] = aqiVal
                }
            }
        }

        val rows = mutableListOf<HourlyRowUi>()
        for (i in 0 until length) {
            val iso = times[i]
            val rawTemp = hourly.temperature2m.getOrNull(i)?.takeIf { it in PersonaConstants.MIN_TEMP_C..PersonaConstants.MAX_TEMP_C }
            val rawApparent = hourly.apparentTemperature.getOrNull(i)?.takeIf { it in PersonaConstants.MIN_TEMP_C..PersonaConstants.MAX_TEMP_C }
            val rawPrecip = hourly.precipitationProbability.getOrNull(i)?.takeIf { it in 0..100 }
            val rawCode = hourly.weatherCode.getOrNull(i)
            val rawWind = hourly.windSpeed10m.getOrNull(i)?.takeIf { it >= 0.0 }
            val rawGusts = hourly.windGusts10m.getOrNull(i)?.takeIf { it >= 0.0 }
            val rawVis = hourly.visibility.getOrNull(i)?.takeIf { it >= PersonaConstants.MIN_VISIBILITY_M }
            val rawUv = hourly.uvIndex.getOrNull(i)?.takeIf { it in PersonaConstants.MIN_UV..PersonaConstants.MAX_UV }
            val isDay = hourly.isDay.getOrNull(i) == 1

            val weatherInfo = WmoWeatherCode.info(rawCode, isDay = isDay)
            val hourLabel = try {
                val dt = LocalDateTime.parse(iso)
                dt.format(DateTimeFormatter.ofPattern("h a", Locale.getDefault()))
            } catch (_: Exception) {
                iso
            }

            rows.add(
                HourlyRowUi(
                    timeIso = iso,
                    timeLabel = hourLabel,
                    tempFormatted = formatTemp(rawTemp),
                    tempRaw = rawTemp,
                    apparentTempRaw = rawApparent,
                    condition = weatherInfo.description,
                    iconKey = weatherInfo.iconKey,
                    isDay = isDay,
                    precipProb = rawPrecip,
                    precipProbFormatted = rawPrecip?.let { "$it%" } ?: "",
                    windKmh = rawWind,
                    windGustsKmh = rawGusts,
                    visibilityM = rawVis,
                    uvIndex = rawUv,
                    aqi = aqiMap[iso],
                    weatherCode = rawCode
                )
            )
        }
        return rows
    }

    private fun buildDaysList(daily: com.weatherengine.app.data.openmeteo.OpenMeteoDailyForecast?): List<DayForecastUi> {
        if (daily == null || daily.time.isEmpty()) return emptyList()
        val minTemps = daily.temperature2mMin
        val maxTemps = daily.temperature2mMax
        val rangeFractions = calculateRangeFractions(minTemps, maxTemps)
        val today = LocalDate.now()

        val count = daily.time.size
        val list = mutableListOf<DayForecastUi>()
        for (i in 0 until count) {
            val dateIso = daily.time[i]
            val code = daily.weatherCode.getOrNull(i)
            val dayWeather = WmoWeatherCode.info(code, isDay = true)
            val maxTemp = maxTemps.getOrNull(i) ?: 0.0
            val minTemp = minTemps.getOrNull(i) ?: 0.0
            val precipProb = daily.precipitationProbabilityMax.getOrNull(i)
            val fractions = rangeFractions.getOrNull(i) ?: Pair(0f, 1f)

            list.add(
                DayForecastUi(
                    dateIso = dateIso,
                    dayLabel = dayLabel(dateIso, i, today),
                    condition = dayWeather.description,
                    iconKey = dayWeather.iconKey,
                    maxTempFormatted = formatTemp(maxTemp),
                    minTempFormatted = formatTemp(minTemp),
                    maxTempRaw = maxTemp,
                    minTempRaw = minTemp,
                    precipitationProbability = precipProb,
                    precipitationProbabilityFormatted = precipProb?.let { "$it%" } ?: "",
                    precipitationSumMm = daily.precipitationSum.getOrNull(i),
                    windSpeedMaxKmh = daily.windSpeed10mMax.getOrNull(i),
                    uvIndexMax = daily.uvIndexMax.getOrNull(i),
                    sunriseFormatted = Formatters.formatClock(daily.sunrise.getOrNull(i)),
                    sunsetFormatted = Formatters.formatClock(daily.sunset.getOrNull(i)),
                    rangeFractionStart = fractions.first,
                    rangeFractionEnd = fractions.second
                )
            )
        }
        return list
    }

    private fun mapPollenInfo(airQualityCurrent: OpenMeteoCurrentAirQuality?): PollenInfo {
        if (airQualityCurrent == null) return PollenInfo()
        val pollens = listOfNotNull(
            airQualityCurrent.grassPollen?.let { "Grass" to it },
            airQualityCurrent.birchPollen?.let { "Birch" to it },
            airQualityCurrent.alderPollen?.let { "Alder" to it },
            airQualityCurrent.ragweedPollen?.let { "Ragweed" to it }
        )
        if (pollens.isEmpty()) {
            return PollenInfo(levelLabel = "Pollen data is not available for this region", isAvailable = false)
        }
        val (name, value) = pollens.maxByOrNull { it.second } ?: return PollenInfo()
        val level = when {
            value < 10.0 -> "Low"
            value < 50.0 -> "Moderate"
            value < 150.0 -> "High"
            else -> "Very High"
        }
        return PollenInfo(
            highestValue = value,
            highestName = name,
            levelLabel = "$level ($name: ${value.roundToInt()} grains/m³)",
            isAvailable = true
        )
    }
}
