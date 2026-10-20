package com.weatherengine.backend.weather

import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.config.WEATHER_CACHE_NAME
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.concurrent.*

@Service
class WeatherCacheService(
    private val openMeteoClient: OpenMeteoClient,
    private val weatherApiClient: WeatherApiClient? = null
) {
    private val logger = LoggerFactory.getLogger(WeatherCacheService::class.java)

    // Bounded thread pool for parallel weather data fetches (<= 16 threads)
    private val fetchExecutor: ExecutorService = Executors.newFixedThreadPool(
        16,
        object : ThreadFactory {
            private val counter = java.util.concurrent.atomic.AtomicInteger(0)
            override fun newThread(r: Runnable): Thread {
                val t = Thread(r, "weather-fetch-${counter.incrementAndGet()}")
                t.isDaemon = true
                return t
            }
        }
    )

    /**
     * Fetches forecast, air quality, and marine data in parallel with a 5s per-call timeout.
     * Caching uses sync = true to prevent cache stampedes under concurrent traffic.
     * Air quality and marine failures degrade to null without failing the entire bundle.
     */
    @Cacheable(
        value = [WEATHER_CACHE_NAME],
        key = "'v4:' + T(java.lang.String).format(T(java.util.Locale).US, '%.2f,%.2f', #lat, #lon)",
        sync = true
    )
    fun getCachedWeatherBundle(lat: Double, lon: Double): WeatherBundleDto {
        val forecastFuture = CompletableFuture.supplyAsync(
            { openMeteoClient.fetchForecast(lat, lon) },
            fetchExecutor
        )

        val airQualityFuture = CompletableFuture.supplyAsync(
            {
                try {
                    openMeteoClient.fetchAirQuality(lat, lon)
                } catch (ex: Exception) {
                    logger.warn("Air quality call failed for ({}, {}): {}", lat, lon, ex.message)
                    null
                }
            },
            fetchExecutor
        )

        val marineFuture = CompletableFuture.supplyAsync(
            {
                try {
                    openMeteoClient.fetchMarine(lat, lon)
                } catch (ex: Exception) {
                    logger.warn("Marine call failed for ({}, {}): {}", lat, lon, ex.message)
                    null
                }
            },
            fetchExecutor
        )

        val forecast = try {
            forecastFuture.get(5, TimeUnit.SECONDS)
        } catch (ex: ExecutionException) {
            throw ex.cause ?: ex
        } catch (ex: TimeoutException) {
            forecastFuture.cancel(true)
            throw ApiException(HttpStatus.GATEWAY_TIMEOUT, "Weather forecast request timed out")
        }

        var airQuality = try {
            airQualityFuture.get(5, TimeUnit.SECONDS)
        } catch (ex: Exception) {
            airQualityFuture.cancel(true)
            logger.warn("Air quality call timed out or failed for ({}, {}): {}", lat, lon, ex.message)
            null
        }

        val marine = try {
            marineFuture.get(5, TimeUnit.SECONDS)
        } catch (ex: Exception) {
            marineFuture.cancel(true)
            logger.warn("Marine call timed out or failed for ({}, {}): {}", lat, lon, ex.message)
            null
        }

        // Secondary fallback for air quality if primary returned null/missing
        if ((airQuality?.current?.usAqi == null) && weatherApiClient?.isConfigured() == true) {
            try {
                val secondary = weatherApiClient.fetchCurrentAndAqi(lat, lon)
                val epaIndex = secondary?.current?.airQuality?.usEpaIndex
                val mappedAqi = when (epaIndex) {
                    1 -> 25
                    2 -> 75
                    3 -> 125
                    4 -> 175
                    5 -> 250
                    6 -> 350
                    else -> null
                }
                if (mappedAqi != null) {
                    airQuality = OpenMeteoAirQualityResponse(
                        current = OpenMeteoCurrentAirQuality(
                            usAqi = mappedAqi,
                            pm25 = secondary?.current?.airQuality?.pm2_5,
                            grassPollen = airQuality?.current?.grassPollen,
                            birchPollen = airQuality?.current?.birchPollen,
                            alderPollen = airQuality?.current?.alderPollen,
                            ragweedPollen = airQuality?.current?.ragweedPollen
                        )
                    )
                }
            } catch (ex: Exception) {
                logger.warn("Secondary WeatherAPI fallback failed: {}", ex.message)
            }
        }

        return WeatherBundleDto(
            forecast = forecast,
            airQuality = airQuality,
            marine = marine,
            fetchedAtMs = System.currentTimeMillis()
        )
    }
}
