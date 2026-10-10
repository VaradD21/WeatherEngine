package com.weatherengine.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.core.UrlUtils
import com.weatherengine.app.core.Validators
import com.weatherengine.app.data.local.SettingsStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverTarget: ServerTarget = ServerTarget.EMULATOR,
    val isEmulatorPreset: Boolean = true,
    val customUrl: String = "",
    val phoneLanUrl: String = SettingsStore.DEFAULT_PHONE_LAN_URL,
    val customUrlError: String? = null,
    val manualLat: String = SettingsStore.DEFAULT_LAT.toString(),
    val manualLon: String = SettingsStore.DEFAULT_LON.toString(),
    val latLonError: String? = null,
    val schoolStart: String = SettingsStore.DEFAULT_SCHOOL_START,
    val schoolEnd: String = SettingsStore.DEFAULT_SCHOOL_END,
    val schoolHoursError: String? = null,
    val feedbackMessage: String? = null
)

class SettingsViewModel(private val settingsStore: SettingsStore) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _navigateAuth = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateAuth: SharedFlow<Unit> = _navigateAuth.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsStore.baseUrlFlow.collect { url ->
                val target = when (url) {
                    SettingsStore.EMULATOR_BASE_URL -> ServerTarget.EMULATOR
                    SettingsStore.USB_ADB_BASE_URL -> ServerTarget.USB_ADB
                    else -> {
                        if (url.startsWith("http://192.168.") || (url.startsWith("http://10.") && url != SettingsStore.EMULATOR_BASE_URL)) {
                            ServerTarget.PHONE_LAN
                        } else {
                            ServerTarget.CUSTOM
                        }
                    }
                }
                val isEmulator = url == SettingsStore.EMULATOR_BASE_URL
                _uiState.update {
                    it.copy(
                        serverTarget = target,
                        isEmulatorPreset = isEmulator,
                        customUrl = if (target == ServerTarget.CUSTOM) url else it.customUrl,
                        phoneLanUrl = if (target == ServerTarget.PHONE_LAN) url else it.phoneLanUrl
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsStore.savedPhoneUrlFlow.collect { savedPhone ->
                _uiState.update { it.copy(phoneLanUrl = savedPhone) }
            }
        }
        viewModelScope.launch {
            settingsStore.manualLatFlow.collect { lat -> _uiState.update { it.copy(manualLat = lat.toString()) } }
        }
        viewModelScope.launch {
            settingsStore.manualLonFlow.collect { lon -> _uiState.update { it.copy(manualLon = lon.toString()) } }
        }
        viewModelScope.launch {
            settingsStore.schoolStartFlow.collect { start -> _uiState.update { it.copy(schoolStart = start) } }
        }
        viewModelScope.launch {
            settingsStore.schoolEndFlow.collect { end -> _uiState.update { it.copy(schoolEnd = end) } }
        }
    }

    fun selectServerPreset(isEmulator: Boolean) {
        selectTarget(if (isEmulator) ServerTarget.EMULATOR else ServerTarget.CUSTOM)
    }

    fun selectTarget(target: ServerTarget) {
        viewModelScope.launch {
            when (target) {
                ServerTarget.EMULATOR -> {
                    settingsStore.setBaseUrl(SettingsStore.EMULATOR_BASE_URL)
                    _uiState.update { it.copy(serverTarget = ServerTarget.EMULATOR, isEmulatorPreset = true, customUrlError = null, feedbackMessage = "Switched to Emulator (10.0.2.2:8080)") }
                }
                ServerTarget.USB_ADB -> {
                    settingsStore.setBaseUrl(SettingsStore.USB_ADB_BASE_URL)
                    _uiState.update { it.copy(serverTarget = ServerTarget.USB_ADB, isEmulatorPreset = false, customUrlError = null, feedbackMessage = "Switched to USB ADB (localhost:8080)") }
                }
                ServerTarget.PHONE_LAN -> {
                    val normalized = UrlUtils.normalizeBaseUrl(_uiState.value.phoneLanUrl) ?: SettingsStore.DEFAULT_PHONE_LAN_URL
                    settingsStore.setBaseUrl(normalized)
                    settingsStore.savePhoneUrl(normalized)
                    _uiState.update { it.copy(serverTarget = ServerTarget.PHONE_LAN, isEmulatorPreset = false, phoneLanUrl = normalized, feedbackMessage = "Switched to Phone LAN ($normalized)") }
                }
                ServerTarget.CUSTOM -> {
                    _uiState.update { it.copy(serverTarget = ServerTarget.CUSTOM, isEmulatorPreset = false) }
                }
            }
        }
    }

    fun onPhoneUrlChanged(url: String) = _uiState.update { it.copy(phoneLanUrl = url) }

    fun savePhoneUrl() {
        val normalized = UrlUtils.normalizeBaseUrl(_uiState.value.phoneLanUrl)
        if (normalized == null) {
            _uiState.update { it.copy(customUrlError = "Enter a valid http:// or https:// URL") }
            return
        }
        viewModelScope.launch {
            settingsStore.savePhoneUrl(normalized)
            settingsStore.setBaseUrl(normalized)
            _uiState.update { it.copy(phoneLanUrl = normalized, serverTarget = ServerTarget.PHONE_LAN, feedbackMessage = "Phone LAN URL saved") }
        }
    }

    fun onCustomUrlChanged(url: String) = _uiState.update { it.copy(customUrl = url, customUrlError = null) }

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

    fun onManualLatChanged(lat: String) = _uiState.update { it.copy(manualLat = lat, latLonError = null) }
    fun onManualLonChanged(lon: String) = _uiState.update { it.copy(manualLon = lon, latLonError = null) }

    fun saveManualCoordinates() {
        val lat = _uiState.value.manualLat.trim()
        val lon = _uiState.value.manualLon.trim()
        if (!Validators.isValidLatLon(lat, lon)) {
            _uiState.update { it.copy(latLonError = "Latitude must be between -90 and 90, longitude between -180 and 180") }
            return
        }
        viewModelScope.launch {
            settingsStore.setManualLocation(lat.toDouble(), lon.toDouble())
            _uiState.update { it.copy(latLonError = null, feedbackMessage = "Coordinates saved") }
        }
    }

    fun onSchoolStartChanged(start: String) = _uiState.update { it.copy(schoolStart = start, schoolHoursError = null) }
    fun onSchoolEndChanged(end: String) = _uiState.update { it.copy(schoolEnd = end, schoolHoursError = null) }

    fun saveSchoolHours() {
        val s = _uiState.value.schoolStart.trim()
        val e = _uiState.value.schoolEnd.trim()
        val error = Validators.validateSchoolHours(s, e)
        if (error != null) {
            _uiState.update { it.copy(schoolHoursError = error) }
            return
        }
        viewModelScope.launch {
            settingsStore.saveSchoolHours(s, e)
            _uiState.update { it.copy(schoolHoursError = null, feedbackMessage = "School hours saved") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsStore.clearAll()
            _navigateAuth.tryEmit(Unit)
        }
    }
}
