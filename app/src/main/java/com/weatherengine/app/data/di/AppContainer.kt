package com.weatherengine.app.data.di

import android.content.Context
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.weatherengine.app.data.api.ApiService
import com.weatherengine.app.data.api.AuthInterceptor
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.local.dataStore
import com.weatherengine.app.data.model.AuthResponse
import com.weatherengine.app.data.model.HomepageResponse
import com.weatherengine.app.data.model.PersonaDto
import com.weatherengine.app.data.openmeteo.OpenMeteoClient
import com.weatherengine.app.data.repository.ForecastRepository
import com.weatherengine.app.data.repository.MockRepository
import com.weatherengine.app.data.repository.OpenMeteoForecastRepository
import com.weatherengine.app.data.repository.RemoteRepository
import com.weatherengine.app.data.repository.WeatherEngineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    val settingsStore: SettingsStore by lazy {
        SettingsStore(context.dataStore)
    }

    private var currentToken: String? = null
    private var isMockMode: Boolean = false
    private var currentBaseUrl: String = SettingsStore.DEFAULT_BASE_URL

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        applicationScope.launch {
            settingsStore.tokenFlow.collect { token ->
                currentToken = token
            }
        }
        applicationScope.launch {
            settingsStore.isMockModeFlow.collect { mock ->
                isMockMode = mock
            }
        }
        applicationScope.launch {
            settingsStore.baseUrlFlow.collect { url ->
                if (url != currentBaseUrl) {
                    currentBaseUrl = url
                    synchronized(this@AppContainer) {
                        cachedApiService = null
                    }
                }
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }

        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor { currentToken })
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Volatile
    private var cachedApiService: ApiService? = null
    private var cachedRetrofitBaseUrl: String? = null

    private fun getApiService(): ApiService {
        val baseUrl = currentBaseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
        synchronized(this) {
            if (cachedApiService == null || cachedRetrofitBaseUrl != baseUrl) {
                cachedRetrofitBaseUrl = baseUrl
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()
                cachedApiService = retrofit.create(ApiService::class.java)
            }
            return cachedApiService!!
        }
    }

    val openMeteoClient: OpenMeteoClient by lazy {
        OpenMeteoClient()
    }

    val forecastRepository: ForecastRepository by lazy {
        OpenMeteoForecastRepository(
            apiService = openMeteoClient.apiService,
            settingsStore = settingsStore
        )
    }

    val mockRepository: MockRepository by lazy {
        MockRepository()
    }

    val remoteRepository: RemoteRepository by lazy {
        RemoteRepository(
            apiServiceProvider = { getApiService() },
            settingsStore = settingsStore
        )
    }

    val repository: WeatherEngineRepository by lazy {
        object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String): NetworkResult<AuthResponse> =
                if (isMockMode) mockRepository.signup(email, password) else remoteRepository.signup(email, password)

            override suspend fun login(email: String, password: String): NetworkResult<AuthResponse> =
                if (isMockMode) mockRepository.login(email, password) else remoteRepository.login(email, password)

            override suspend fun getPersonas(): NetworkResult<List<PersonaDto>> =
                if (isMockMode) mockRepository.getPersonas() else remoteRepository.getPersonas()

            override suspend fun setPersonas(codes: List<String>): NetworkResult<List<PersonaDto>> =
                if (isMockMode) mockRepository.setPersonas(codes) else remoteRepository.setPersonas(codes)

            override suspend fun getHomepage(lat: Double, lon: Double): NetworkResult<HomepageResponse> =
                if (isMockMode) mockRepository.getHomepage(lat, lon) else remoteRepository.getHomepage(lat, lon)
        }
    }
}
