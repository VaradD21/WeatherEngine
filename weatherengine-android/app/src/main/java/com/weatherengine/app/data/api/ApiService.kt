package com.weatherengine.app.data.api

import com.weatherengine.app.data.model.AuthResponse
import com.weatherengine.app.data.model.HomepageResponse
import com.weatherengine.app.data.model.LoginRequest
import com.weatherengine.app.data.model.PersonaDto
import com.weatherengine.app.data.model.SetPersonasRequest
import com.weatherengine.app.data.model.SignupRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
        @Query("lon") lon: Double
    ): Response<HomepageResponse>
}
