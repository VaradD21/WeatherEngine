package com.weatherengine.app.data.repository

import com.weatherengine.app.core.Formatters
import com.weatherengine.app.core.forecast.ForecastMapper
import com.weatherengine.app.core.forecast.ForecastUi
import com.weatherengine.app.data.api.FailureKind
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.openmeteo.OpenMeteoAirQualityResponse
import com.weatherengine.app.data.openmeteo.OpenMeteoApiService
import com.weatherengine.app.data.openmeteo.OpenMeteoClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale

class OpenMeteoForecastRepository(
    private val apiService: OpenMeteoApiService,
    private val settingsStore: SettingsStore,
    private val airQualityUrl: String = OpenMeteoClient.DEFAULT_AIR_QUALITY_URL
) : ForecastRepository {

    companion object {
        const val CACHE_VALIDITY_MS = 15 * 60 * 1000L // 15 minutes
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getForecast(
        lat: Double,
        lon: Double,
        locationLabel: String,
        forceRefresh: Boolean
    ): NetworkResult<ForecastUi> {
        val locKey = String.format(Locale.US, "%.2f,%.2f", lat, lon)
        val now = System.currentTimeMillis()

        // 1. Check in-memory / DataStore cache if not forcing refresh
        if (!forceRefresh) {
            val cached = getCachedForecast(locKey)
            if (cached != null && (now - cached.timestampMs) < CACHE_VALIDITY_MS) {
                val updatedUi = cached.copy(
                    locationLabel = locationLabel,
                    updatedAtLabel = ForecastMapper.formatUpdatedAt(cached.timestampMs, now),
                    isCached = true
                )
                return NetworkResult.Success(updatedUi)
            }
        }

        // 2. Fetch fresh forecast & air quality concurrently
        return try {
            coroutineScope {
                val forecastDeferred = async {
                    apiService.getForecast(latitude = lat, longitude = lon)
                }
                val aqiDeferred = async {
                    try {
                        val resp = apiService.getAirQuality(
                            url = airQualityUrl,
                            latitude = lat,
                            longitude = lon
                        )
                        if (resp.isSuccessful) resp.body() else null
                    } catch (_: Exception) {
                        null
                    }
                }

                val forecastResponse = forecastDeferred.await()
                val aqiResponse: OpenMeteoAirQualityResponse? = aqiDeferred.await()

                if (forecastResponse.isSuccessful) {
                    val body = forecastResponse.body()
                    if (body != null && body.current != null) {
                        val forecastUi = ForecastMapper.mapToForecastUi(
                            forecastResponse = body,
                            airQualityResponse = aqiResponse,
                            locationLabel = locationLabel,
                            timestampMs = now
                        )

                        // Persist into cache
                        try {
                            val serialized = json.encodeToString(forecastUi)
                            settingsStore.cacheForecast(locKey, serialized, now)
                        } catch (_: Exception) {}

                        NetworkResult.Success(forecastUi)
                    } else {
                        NetworkResult.Failure(FailureKind.Parse, "Empty or invalid response from weather service")
                    }
                } else {
                    handleHttpError(forecastResponse.code(), locKey, locationLabel, now)
                }
            }
        } catch (e: Exception) {
            handleException(e, locKey, locationLabel, now)
        }
    }

    private suspend fun getCachedForecast(locKey: String): ForecastUi? {
        return try {
            val schema = settingsStore.cachedForecastSchemaFlow.first()
            if (schema != SettingsStore.CURRENT_FORECAST_SCHEMA_VERSION) {
                return null
            }
            val cachedKey = settingsStore.cachedForecastLocKeyFlow.first()
            if (cachedKey == locKey) {
                val cachedJson = settingsStore.cachedForecastJsonFlow.first()
                if (!cachedJson.isNullOrBlank()) {
                    json.decodeFromString<ForecastUi>(cachedJson)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun handleHttpError(
        statusCode: Int,
        locKey: String,
        locationLabel: String,
        now: Long
    ): NetworkResult<ForecastUi> {
        val cached = getCachedForecast(locKey)
        if (cached != null) {
            val mins = Formatters.minutesAgo(cached.timestampMs, now)
            return NetworkResult.Success(
                cached.copy(
                    locationLabel = locationLabel,
                    updatedAtLabel = "Showing data from $mins min ago (offline)",
                    isCached = true
                )
            )
        }

        val kind = when (statusCode) {
            400 -> FailureKind.Client
            404 -> FailureKind.Client
            429 -> FailureKind.Server
            in 500..599 -> FailureKind.Server
            else -> FailureKind.Client
        }
        return NetworkResult.Failure(kind, "Weather service error ($statusCode)")
    }

    private suspend fun handleException(
        e: Exception,
        locKey: String,
        locationLabel: String,
        now: Long
    ): NetworkResult<ForecastUi> {
        val cached = getCachedForecast(locKey)
        if (cached != null) {
            val mins = Formatters.minutesAgo(cached.timestampMs, now)
            return NetworkResult.Success(
                cached.copy(
                    locationLabel = locationLabel,
                    updatedAtLabel = "Showing data from $mins min ago (offline)",
                    isCached = true
                )
            )
        }

        val (kind, message) = when (e) {
            is UnknownHostException -> Pair(FailureKind.Network, "No internet connection")
            is SocketTimeoutException -> Pair(FailureKind.Timeout, "Weather request timed out")
            is IOException -> Pair(FailureKind.Network, "Network connection issue: ${e.localizedMessage ?: "Unknown"}")
            else -> Pair(FailureKind.Client, e.localizedMessage ?: "Unexpected error")
        }
        return NetworkResult.Failure(kind, message)
    }
}
