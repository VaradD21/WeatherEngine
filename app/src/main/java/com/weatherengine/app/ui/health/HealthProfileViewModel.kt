package com.weatherengine.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.model.HealthProfileDto
import com.weatherengine.app.data.repository.WeatherEngineRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HealthProfileUiState(
    val hasAsthma: Boolean = false,
    val hasAllergies: Boolean = false,
    val hasSkinSensitivity: Boolean = false,
    val aqiThreshold: Int = 100,
    val uvThreshold: Int = 6,
    val humidityThreshold: Int = 70,
    val pollenThreshold: String = "MODERATE",
    val alertsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HealthProfileViewModel(
    private val repository: WeatherEngineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthProfileUiState())
    val uiState: StateFlow<HealthProfileUiState> = _uiState.asStateFlow()

    private val _savedSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedSuccess: SharedFlow<Unit> = _savedSuccess.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = repository.getHealthProfile()) {
                is NetworkResult.Success -> {
                    val p = res.data
                    _uiState.update {
                        it.copy(
                            hasAsthma = p.hasAsthma,
                            hasAllergies = p.hasAllergies,
                            hasSkinSensitivity = p.hasSkinSensitivity,
                            aqiThreshold = p.aqiThreshold,
                            uvThreshold = p.uvThreshold,
                            humidityThreshold = p.humidityThreshold,
                            pollenThreshold = p.pollenThreshold,
                            alertsEnabled = p.alertsEnabled,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun toggleAsthma(enabled: Boolean) = _uiState.update { it.copy(hasAsthma = enabled) }
    fun toggleAllergies(enabled: Boolean) = _uiState.update { it.copy(hasAllergies = enabled) }
    fun toggleSkinSensitivity(enabled: Boolean) = _uiState.update { it.copy(hasSkinSensitivity = enabled) }

    fun setAqiThreshold(value: Int) = _uiState.update { it.copy(aqiThreshold = value) }
    fun setUvThreshold(value: Int) = _uiState.update { it.copy(uvThreshold = value) }
    fun setHumidityThreshold(value: Int) = _uiState.update { it.copy(humidityThreshold = value) }
    fun setPollenThreshold(level: String) = _uiState.update { it.copy(pollenThreshold = level) }
    fun toggleAlertsEnabled(enabled: Boolean) = _uiState.update { it.copy(alertsEnabled = enabled) }

    fun saveProfile() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val dto = HealthProfileDto(
                hasAsthma = s.hasAsthma,
                hasAllergies = s.hasAllergies,
                hasSkinSensitivity = s.hasSkinSensitivity,
                aqiThreshold = s.aqiThreshold,
                uvThreshold = s.uvThreshold,
                humidityThreshold = s.humidityThreshold,
                pollenThreshold = s.pollenThreshold,
                alertsEnabled = s.alertsEnabled
            )
            when (val res = repository.updateHealthProfile(dto)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _savedSuccess.tryEmit(Unit)
                }
                is NetworkResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }
}
