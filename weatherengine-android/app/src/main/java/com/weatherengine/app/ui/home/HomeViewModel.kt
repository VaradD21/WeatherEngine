package com.weatherengine.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.core.Formatters
import com.weatherengine.app.core.WidgetMapper
import com.weatherengine.app.core.forecast.ForecastUi
import com.weatherengine.app.core.models.WidgetUi
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.location.LocationProvider
import com.weatherengine.app.data.model.HomepageResponse
import com.weatherengine.app.data.repository.ForecastRepository
import com.weatherengine.app.data.repository.WeatherEngineRepository
import com.weatherengine.app.persona.PersonaRules
import com.weatherengine.app.persona.PersonaWidgetType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

sealed class ForecastUiState {
    data object Loading : ForecastUiState()
    data class Success(val forecast: ForecastUi) : ForecastUiState()
    data class Error(val message: String) : ForecastUiState()
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Content(
        val widgets: List<WidgetUi>,
        val locationLabel: String,
        val offlineBannerMessage: String? = null
    ) : HomeUiState()
    data object Empty : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data object Guest : HomeUiState()
}

class HomeViewModel(
    private val forecastRepository: ForecastRepository,
    private val repository: WeatherEngineRepository,
    private val settingsStore: SettingsStore,
    private val locationProvider: LocationProvider? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    companion object {
        const val USE_BACKEND_WIDGETS = false
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _forecastState = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val forecastState: StateFlow<ForecastUiState> = _forecastState.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _locationLabel = MutableStateFlow("Locating…")
    val locationLabel: StateFlow<String> = _locationLabel.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isMockMode = MutableStateFlow(false)
    val isMockMode: StateFlow<Boolean> = _isMockMode.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _selectedPersonas = MutableStateFlow<Set<String>>(emptySet())
    val selectedPersonas: StateFlow<Set<String>> = _selectedPersonas.asStateFlow()

    private val _activePersonaWidgets = MutableStateFlow<List<PersonaWidgetType>>(emptyList())
    val activePersonaWidgets: StateFlow<List<PersonaWidgetType>> = _activePersonaWidgets.asStateFlow()

    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            settingsStore.isMockModeFlow.collect { mock ->
                _isMockMode.value = mock
            }
        }
        viewModelScope.launch {
            settingsStore.tokenFlow.collect { token ->
                _isAuthenticated.value = !token.isNullOrBlank()
            }
        }
        viewModelScope.launch {
            settingsStore.selectedPersonasFlow.collect { personas ->
                _selectedPersonas.value = personas
                _activePersonaWidgets.value = PersonaRules.mergedWidgets(personas)
            }
        }
        loadData()
    }

    fun loadData(isUserRefresh: Boolean = false) {
        fetchJob?.cancel()

        if (isUserRefresh) {
            _isRefreshing.value = true
        } else {
            if (_forecastState.value !is ForecastUiState.Success) {
                _forecastState.value = ForecastUiState.Loading
            }
            if (_uiState.value !is HomeUiState.Content) {
                _uiState.value = HomeUiState.Loading
            }
        }

        fetchJob = viewModelScope.launch(ioDispatcher) {
            try {
                var lat = settingsStore.manualLatFlow.first()
                var lon = settingsStore.manualLonFlow.first()

                val location = try {
                    locationProvider?.getCurrentLocation()
                } catch (_: Exception) {
                    null
                }

                val resolvedLabel: String = if (location != null) {
                    lat = location.latitude
                    lon = location.longitude
                    location.label
                } else {
                    val placeName = locationProvider?.getPlaceName(lat, lon)
                    if (!placeName.isNullOrBlank() && !placeName.contains("°")) {
                        placeName
                    } else if (abs(lat - SettingsStore.DEFAULT_LAT) < 0.1 && abs(lon - SettingsStore.DEFAULT_LON) < 0.1) {
                        "Mumbai, Maharashtra"
                    } else if (!placeName.isNullOrBlank()) {
                        placeName
                    } else {
                        "Mumbai, Maharashtra"
                    }
                }

                _locationLabel.value = resolvedLabel

                val token = settingsStore.tokenFlow.first()
                val mockMode = settingsStore.isMockModeFlow.first()

                // Concurrent fetch for Forecast and optional Backend Widgets
                val forecastDeferred = async {
                    forecastRepository.getForecast(
                        lat = lat,
                        lon = lon,
                        locationLabel = resolvedLabel,
                        forceRefresh = isUserRefresh
                    )
                }

                val widgetsDeferred = async {
                    if (USE_BACKEND_WIDGETS && (mockMode || !token.isNullOrBlank())) {
                        repository.getHomepage(lat, lon)
                    } else {
                        null
                    }
                }

                // Handle Forecast Result
                when (val forecastResult = forecastDeferred.await()) {
                    is NetworkResult.Success -> {
                        _forecastState.value = ForecastUiState.Success(forecastResult.data)
                    }
                    is NetworkResult.Failure -> {
                        _forecastState.value = ForecastUiState.Error(forecastResult.message)
                    }
                }

                // Handle Widgets Result
                val widgetResult = widgetsDeferred.await()
                if (widgetResult != null) {
                    when (widgetResult) {
                        is NetworkResult.Success -> {
                            val response = widgetResult.data
                            if (response.widgets.isEmpty()) {
                                _uiState.value = HomeUiState.Empty
                            } else {
                                val mapped = response.widgets.map { dto ->
                                    WidgetMapper.mapWidget(dto.type, dto.data)
                                }
                                try {
                                    val jsonStr = json.encodeToString(response)
                                    settingsStore.cacheHomepage(jsonStr, System.currentTimeMillis())
                                } catch (_: Exception) {}

                                _uiState.value = HomeUiState.Content(
                                    widgets = mapped,
                                    locationLabel = resolvedLabel,
                                    offlineBannerMessage = null
                                )
                            }
                        }
                        is NetworkResult.Failure -> {
                            val cachedJson = settingsStore.cachedHomepageJsonFlow.first()
                            val cachedTime = settingsStore.cachedHomepageTimeFlow.first()

                            if (!cachedJson.isNullOrBlank() && cachedTime != null) {
                                try {
                                    val cachedResponse = json.decodeFromString<HomepageResponse>(cachedJson)
                                    val mapped = cachedResponse.widgets.map { dto ->
                                        WidgetMapper.mapWidget(dto.type, dto.data)
                                    }
                                    val minutes = Formatters.minutesAgo(cachedTime, System.currentTimeMillis())
                                    _uiState.value = HomeUiState.Content(
                                        widgets = mapped,
                                        locationLabel = resolvedLabel,
                                        offlineBannerMessage = "Showing data from $minutes min ago (offline)"
                                    )
                                } catch (_: Exception) {
                                    _uiState.value = HomeUiState.Error(widgetResult.message)
                                }
                            } else {
                                _uiState.value = HomeUiState.Error(widgetResult.message)
                            }
                        }
                    }
                } else {
                    _uiState.value = HomeUiState.Guest
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadHomepage(isUserRefresh: Boolean = false) {
        loadData(isUserRefresh)
    }
}
