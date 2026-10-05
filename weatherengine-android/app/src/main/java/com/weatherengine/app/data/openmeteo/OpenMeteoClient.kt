package com.weatherengine.app.data.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface OpenMeteoApiService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_gusts_10m",
        @Query("hourly") hourly: String = "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m,wind_gusts_10m,visibility,uv_index,is_day",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,uv_index_max,sunrise,sunset",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): Response<OpenMeteoForecastResponse>

    @GET
    suspend fun getAirQuality(
        @Url url: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,pm2_5,grass_pollen,birch_pollen,alder_pollen,ragweed_pollen",
        @Query("hourly") hourly: String = "us_aqi,pm2_5",
        @Query("timezone") timezone: String = "auto"
    ): Response<OpenMeteoAirQualityResponse>
}

class OpenMeteoClient(
    baseUrl: String = DEFAULT_BASE_URL,
    okHttpClient: OkHttpClient? = null
) {

    companion object {
        const val DEFAULT_BASE_URL = "https://api.open-meteo.com/"
        const val DEFAULT_AIR_QUALITY_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"
        const val USER_AGENT = "WeatherEngine-Android/1.0"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val client: OkHttpClient = okHttpClient ?: OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    val apiService: OpenMeteoApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenMeteoApiService::class.java)
}

@Serializable
data class OpenMeteoForecastResponse(
    val current: OpenMeteoCurrentWeather? = null,
    val hourly: OpenMeteoHourlyForecast? = null,
    val daily: OpenMeteoDailyForecast? = null
)

@Serializable
data class OpenMeteoCurrentWeather(
    val time: String? = null,
    @SerialName("temperature_2m") val temperature2m: Double? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("wind_speed_10m") val windSpeed10m: Double? = null,
    @SerialName("wind_gusts_10m") val windGusts10m: Double? = null
)

@Serializable
data class OpenMeteoHourlyForecast(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val relativeHumidity2m: List<Int?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @SerialName("wind_gusts_10m") val windGusts10m: List<Double?> = emptyList(),
    val visibility: List<Double?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
    @SerialName("is_day") val isDay: List<Int?> = emptyList()
)

@Serializable
data class OpenMeteoDailyForecast(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeed10mMax: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList()
)

@Serializable
data class OpenMeteoAirQualityResponse(
    val current: OpenMeteoCurrentAirQuality? = null,
    val hourly: OpenMeteoHourlyAirQuality? = null
)

@Serializable
data class OpenMeteoCurrentAirQuality(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("grass_pollen") val grassPollen: Double? = null,
    @SerialName("birch_pollen") val birchPollen: Double? = null,
    @SerialName("alder_pollen") val alderPollen: Double? = null,
    @SerialName("ragweed_pollen") val ragweedPollen: Double? = null
)

@Serializable
data class OpenMeteoHourlyAirQuality(
    val time: List<String> = emptyList(),
    @SerialName("us_aqi") val usAqi: List<Int?> = emptyList()
)
