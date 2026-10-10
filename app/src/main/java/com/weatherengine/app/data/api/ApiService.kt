package com.weatherengine.app.data.api

import com.weatherengine.app.data.model.AuthResponse
import com.weatherengine.app.data.model.HealthProfileDto
import com.weatherengine.app.data.model.HomepageResponse
import com.weatherengine.app.data.model.LoginRequest
import com.weatherengine.app.data.model.PersonaDto
import com.weatherengine.app.data.model.SetPersonasRequest
import com.weatherengine.app.data.model.SignupRequest
import com.weatherengine.app.data.model.WeatherBundleDto
import okhttp3.Interceptor
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Failure(val kind: FailureKind, val message: String) : NetworkResult<Nothing>()
}

enum class FailureKind {
    Network,
    Timeout,
    Unauthorized,
    Client,
    Server,
    Parse
}

class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Do not attach Authorization header to authentication endpoints
        if (path.contains("/api/auth/")) {
            return chain.proceed(originalRequest)
        }

        val token = tokenProvider()
        return if (!token.isNullOrBlank()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}

interface ApiService {

    @POST("api/auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @GET("api/users/me/personas")
    suspend fun getPersonas(): Response<List<PersonaDto>>

    @POST("api/users/me/personas")
    suspend fun setPersonas(
        @Body request: SetPersonasRequest
    ): Response<List<PersonaDto>>

    @GET("api/homepage")
    suspend fun getHomepage(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("schoolStart") schoolStart: String? = null,
        @Query("schoolEnd") schoolEnd: String? = null
    ): Response<HomepageResponse>

    @GET("api/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Response<WeatherBundleDto>

    @GET("api/users/me/health-profile")
    suspend fun getHealthProfile(): Response<HealthProfileDto>

    @PUT("api/users/me/health-profile")
    suspend fun updateHealthProfile(
        @Body request: HealthProfileDto
    ): Response<HealthProfileDto>
}
