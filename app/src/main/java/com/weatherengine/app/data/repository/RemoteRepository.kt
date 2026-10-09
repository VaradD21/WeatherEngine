package com.weatherengine.app.data.repository

import com.weatherengine.app.core.ErrorUtils
import com.weatherengine.app.data.api.ApiService
import com.weatherengine.app.data.api.FailureKind
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.model.AuthResponse
import com.weatherengine.app.data.model.HomepageResponse
import com.weatherengine.app.data.model.LoginRequest
import com.weatherengine.app.data.model.PersonaDto
import com.weatherengine.app.data.model.SetPersonasRequest
import com.weatherengine.app.data.model.SignupRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

interface WeatherEngineRepository {
    suspend fun signup(email: String, password: String): NetworkResult<AuthResponse>
    suspend fun login(email: String, password: String): NetworkResult<AuthResponse>
    suspend fun getPersonas(): NetworkResult<List<PersonaDto>>
    suspend fun setPersonas(codes: List<String>): NetworkResult<List<PersonaDto>>
    suspend fun getHomepage(lat: Double, lon: Double): NetworkResult<HomepageResponse> =
        getHomepage(lat, lon, null, null)
    suspend fun getHomepage(
        lat: Double,
        lon: Double,
        schoolStart: String?,
        schoolEnd: String?
    ): NetworkResult<HomepageResponse> =
        getHomepage(lat, lon)
}

class RemoteRepository(
    private val apiServiceProvider: () -> ApiService,
    private val settingsStore: SettingsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherEngineRepository {

    private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvents: SharedFlow<Unit> = _sessionExpiredEvents.asSharedFlow()

    override suspend fun signup(email: String, password: String): NetworkResult<AuthResponse> {
        return safeApiCall(isProtected = false) {
            apiServiceProvider().signup(SignupRequest(email, password))
        }
    }

    override suspend fun login(email: String, password: String): NetworkResult<AuthResponse> {
        return safeApiCall(isProtected = false) {
            apiServiceProvider().login(LoginRequest(email, password))
        }
    }

    override suspend fun getPersonas(): NetworkResult<List<PersonaDto>> {
        return safeApiCall(isProtected = true) {
            apiServiceProvider().getPersonas()
        }
    }

    override suspend fun setPersonas(codes: List<String>): NetworkResult<List<PersonaDto>> {
        return safeApiCall(isProtected = true) {
            apiServiceProvider().setPersonas(SetPersonasRequest(codes))
        }
    }

    override suspend fun getHomepage(
        lat: Double,
        lon: Double,
        schoolStart: String?,
        schoolEnd: String?
    ): NetworkResult<HomepageResponse> {
        return safeApiCall(isProtected = true) {
            apiServiceProvider().getHomepage(lat, lon, schoolStart, schoolEnd)
        }
    }

    private suspend fun <T> safeApiCall(
        isProtected: Boolean,
        apiCall: suspend () -> Response<T>
    ): NetworkResult<T> = withContext(ioDispatcher) {
        try {
            val response = apiCall()
            val code = response.code()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.Failure(FailureKind.Parse, "Empty response body from server")
                }
            } else {
                val errorBodyString = try {
                    response.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }

                if (code == 401 && isProtected) {
                    settingsStore.clearToken()
                    _sessionExpiredEvents.tryEmit(Unit)
                    NetworkResult.Failure(
                        FailureKind.Unauthorized,
                        "Session expired, please log in again"
                    )
                } else {
                    val errorMessage = ErrorUtils.describeApiError(code, errorBodyString)
                    val kind = when (code) {
                        401 -> FailureKind.Unauthorized
                        in 400..499 -> FailureKind.Client
                        in 500..599 -> FailureKind.Server
                        else -> FailureKind.Client
                    }
                    NetworkResult.Failure(kind, errorMessage)
                }
            }
        } catch (_: SocketTimeoutException) {
            NetworkResult.Failure(FailureKind.Timeout, "Server took too long to respond")
        } catch (_: IOException) {
            NetworkResult.Failure(FailureKind.Network, "Cannot reach server. Is it running?")
        } catch (_: SerializationException) {
            NetworkResult.Failure(FailureKind.Parse, "Failed to parse server response")
        } catch (e: Exception) {
            NetworkResult.Failure(FailureKind.Client, e.message ?: "An unexpected error occurred")
        }
    }
}
