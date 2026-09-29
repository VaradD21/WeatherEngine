package com.weatherengine.app.data.openmeteo

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

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
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    val apiService: OpenMeteoApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenMeteoApiService::class.java)
}
