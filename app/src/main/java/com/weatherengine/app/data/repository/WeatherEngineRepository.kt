package com.weatherengine.app.data.repository

import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.model.AuthResponse
import com.weatherengine.app.data.model.HomepageResponse
import com.weatherengine.app.data.model.PersonaDto

interface WeatherEngineRepository {
    suspend fun signup(email: String, password: String): NetworkResult<AuthResponse>
    suspend fun login(email: String, password: String): NetworkResult<AuthResponse>
    suspend fun getPersonas(): NetworkResult<List<PersonaDto>>
    suspend fun setPersonas(codes: List<String>): NetworkResult<List<PersonaDto>>
    suspend fun getHomepage(lat: Double, lon: Double): NetworkResult<HomepageResponse>
}
