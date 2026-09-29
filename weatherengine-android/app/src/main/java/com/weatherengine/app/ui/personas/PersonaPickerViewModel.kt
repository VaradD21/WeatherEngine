package com.weatherengine.app.ui.personas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.data.local.SettingsStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonaOption(
    val code: String,
    val title: String,
    val description: String
)

data class PersonaPickerUiState(
    val selectedCodes: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isMockMode: Boolean = false
)

class PersonaPickerViewModel(
    private val settingsStore: SettingsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val availablePersonas = listOf(
        PersonaOption(
            code = "health_conscious",
            title = "Health-conscious",
            description = "AQI, UV index, and heat alerts tailored for health and respiratory safety."
        ),
        PersonaOption(
            code = "outdoor_fitness",
            title = "Outdoor Fitness",
            description = "Wind speed, storm/fog alerts, humidity, and UV for sports and workouts."
        ),
        PersonaOption(
            code = "commuter",
            title = "Commuter",
            description = "Road visibility, commute traffic updates, and sudden transit alerts."
        )
    )

    private val _uiState = MutableStateFlow(PersonaPickerUiState())
    val uiState: StateFlow<PersonaPickerUiState> = _uiState.asStateFlow()

    private val _navigateHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateHome: SharedFlow<Unit> = _navigateHome.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsStore.isMockModeFlow.collect { mock ->
                _uiState.update { it.copy(isMockMode = mock) }
            }
        }
        loadCurrentPersonas()
    }

    fun loadCurrentPersonas() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            val saved = settingsStore.selectedPersonasFlow.first()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedCodes = saved
                )
            }
        }
    }

    fun toggleSelection(code: String) {
        _uiState.update { state ->
            val updated = state.selectedCodes.toMutableSet()
            if (updated.contains(code)) {
                updated.remove(code)
            } else {
                updated.add(code)
            }
            state.copy(selectedCodes = updated, errorMessage = null)
        }
    }

    fun save() {
        val currentSelected = _uiState.value.selectedCodes
        if (currentSelected.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one persona") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch(ioDispatcher) {
            try {
                settingsStore.saveSelectedPersonas(currentSelected)
                _uiState.update { it.copy(isSaving = false) }
                _navigateHome.tryEmit(Unit)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.localizedMessage ?: "Failed to save personas"
                    )
                }
            }
        }
    }
}
