package com.weatherengine.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.core.UrlUtils
import com.weatherengine.app.core.Validators
import com.weatherengine.app.data.local.SettingsStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = SettingsStore.DEFAULT_BASE_URL,
    val isEmulatorPreset: Boolean = true,
    val customUrl: String = "",
    val customUrlError: String? = null,
    val isMockMode: Boolean = false,
    val manualLat: String = SettingsStore.DEFAULT_LAT.toString(),
    val manualLon: String = SettingsStore.DEFAULT_LON.toString(),
    val latLonError: String? = null,
    val feedbackMessage: String? = null
)

class SettingsViewModel(
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _navigateAuth = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateAuth: SharedFlow<Unit> = _navigateAuth.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsStore.baseUrlFlow.collect { url ->
                val isEmulator = url == SettingsStore.DEFAULT_BASE_URL
                _uiState.update {
                    it.copy(
                        baseUrl = url,
                        isEmulatorPreset = isEmulator,
                        customUrl = if (!isEmulator) url else it.customUrl
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsStore.isMockModeFlow.collect { mock ->
                _uiState.update { it.copy(isMockMode = mock) }
            }
        }
        viewModelScope.launch {
            settingsStore.manualLatFlow.collect { lat ->
                _uiState.update { it.copy(manualLat = lat.toString()) }
            }
        }
        viewModelScope.launch {
            settingsStore.manualLonFlow.collect { lon ->
                _uiState.update { it.copy(manualLon = lon.toString()) }
            }
        }
    }

    fun selectServerPreset(isEmulator: Boolean) {
        viewModelScope.launch {
            if (isEmulator) {
                settingsStore.setBaseUrl(SettingsStore.DEFAULT_BASE_URL)
                _uiState.update { it.copy(isEmulatorPreset = true, customUrlError = null) }
            } else {
                _uiState.update { it.copy(isEmulatorPreset = false) }
            }
        }
    }

    fun onCustomUrlChanged(url: String) {
        _uiState.update { it.copy(customUrl = url, customUrlError = null) }
    }

    fun saveCustomUrl() {
        val normalized = UrlUtils.normalizeBaseUrl(_uiState.value.customUrl)
        if (normalized == null) {
            _uiState.update { it.copy(customUrlError = "Enter a valid http:// or https:// URL") }
            return
        }
        viewModelScope.launch {
            settingsStore.setBaseUrl(normalized)
            _uiState.update { it.copy(customUrl = normalized, customUrlError = null, feedbackMessage = "Server URL updated") }
        }
    }

    fun setMockMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setMockMode(enabled)
        }
    }

    fun onManualLatChanged(lat: String) {
        _uiState.update { it.copy(manualLat = lat, latLonError = null) }
    }

    fun onManualLonChanged(lon: String) {
        _uiState.update { it.copy(manualLon = lon, latLonError = null) }
    }

    fun saveManualCoordinates() {
        val lat = _uiState.value.manualLat.trim()
        val lon = _uiState.value.manualLon.trim()
        if (!Validators.isValidLatLon(lat, lon)) {
            _uiState.update { it.copy(latLonError = "Latitude must be between -90 and 90, longitude between -180 and 180") }
            return
        }
        val latDouble = lat.toDouble()
        val lonDouble = lon.toDouble()
        viewModelScope.launch {
            settingsStore.setManualLocation(latDouble, lonDouble)
            _uiState.update { it.copy(latLonError = null, feedbackMessage = "Coordinates saved") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsStore.clearAll()
            _navigateAuth.tryEmit(Unit)
        }
    }
}
