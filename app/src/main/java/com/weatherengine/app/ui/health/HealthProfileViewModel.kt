package com.weatherengine.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.model.HealthProfileDto
import com.weatherengine.app.data.repository.WeatherEngineRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PersonaChoice(
    val code: String,
    val title: String,
    val subtitle: String
)

data class HealthProfileUiState(
    val hasAsthma: Boolean = false,
    val hasAllergies: Boolean = false,
    val hasSkinSensitivity: Boolean = false,
    val hasMigraines: Boolean = false,
    val hasJointPain: Boolean = false,
    val hasHeatSensitivity: Boolean = false,
    val aqiThreshold: Int = 100,
    val uvThreshold: Int = 6,
    val humidityThreshold: Int = 70,
    val pollenThreshold: String = "MODERATE",
    val alertsEnabled: Boolean = true,
    val selectedPersonas: Set<String> = setOf("health_conscious"),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HealthProfileViewModel(
    private val repository: WeatherEngineRepository,
    private val settingsStore: SettingsStore? = null
) : ViewModel() {

    val availablePersonas = listOf(
        PersonaChoice("health_conscious", "Health Focus", "AQI, pollen, UV & heat health advisories"),
        PersonaChoice("outdoor_fitness", "Outdoor Fitness", "Best running hours, wind speed & heat alerts"),
        PersonaChoice("commuter", "Daily Commute", "Traffic conditions, road visibility & fog alerts"),
        PersonaChoice("gardener", "Agriculture & Gardening", "Soil moisture, frost alerts & planting advice"),
        PersonaChoice("beachgoer", "Beach & Surfing", "Sea temperature, tides & wave conditions"),
        PersonaChoice("traveler", "Travel & Flights", "Saved destinations, flight alerts & packing tips"),
        PersonaChoice("parent", "Family & School", "School runs, rain lookahead & severe weather"),
        PersonaChoice("event_planner", "Event Planning", "Extended forecast, rain odds & comfort score")
    )

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
            val currentPersonas = settingsStore?.selectedPersonasFlow?.firstOrNull() ?: emptySet()
            if (currentPersonas.isNotEmpty()) {
                _uiState.update { it.copy(selectedPersonas = currentPersonas) }
            }

            when (val res = repository.getHealthProfile()) {
                is NetworkResult.Success -> {
                    val p = res.data
                    _uiState.update {
                        it.copy(
                            hasAsthma = p.hasAsthma,
                            hasAllergies = p.hasAllergies,
                            hasSkinSensitivity = p.hasSkinSensitivity,
                            hasMigraines = p.hasMigraines,
                            hasJointPain = p.hasJointPain,
                            hasHeatSensitivity = p.hasHeatSensitivity,
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
    fun toggleMigraines(enabled: Boolean) = _uiState.update { it.copy(hasMigraines = enabled) }
    fun toggleJointPain(enabled: Boolean) = _uiState.update { it.copy(hasJointPain = enabled) }
    fun toggleHeatSensitivity(enabled: Boolean) = _uiState.update { it.copy(hasHeatSensitivity = enabled) }

    fun togglePersona(code: String) = _uiState.update { state ->
        val updated = state.selectedPersonas.toMutableSet()
        if (!updated.remove(code)) {
            updated.add(code)
        }
        state.copy(selectedPersonas = updated)
    }

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
                hasMigraines = s.hasMigraines,
                hasJointPain = s.hasJointPain,
                hasHeatSensitivity = s.hasHeatSensitivity,
                aqiThreshold = s.aqiThreshold,
                uvThreshold = s.uvThreshold,
                humidityThreshold = s.humidityThreshold,
                pollenThreshold = s.pollenThreshold,
                alertsEnabled = s.alertsEnabled
            )

            // Save Health Profile to backend
            val healthRes = repository.updateHealthProfile(dto)

            // Save Selected Personas to backend & local store
            val personasToSave = if (s.selectedPersonas.isEmpty()) setOf("health_conscious") else s.selectedPersonas
            runCatching {
                repository.setPersonas(personasToSave.toList())
                settingsStore?.saveSelectedPersonas(personasToSave)
            }

            when (healthRes) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _savedSuccess.tryEmit(Unit)
                }
                is NetworkResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = healthRes.message) }
                }
            }
        }
    }
}
