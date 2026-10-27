package com.weatherengine.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.core.UrlUtils
import com.weatherengine.app.core.Validators
import com.weatherengine.app.core.alert.HomeRainChecker
import com.weatherengine.app.core.notification.AlertNotificationManager
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.location.LocationProvider
import com.weatherengine.app.data.openmeteo.OpenMeteoApiService
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
    val feedbackMessage: String? = null,
    val homeRainAlertEnabled: Boolean = false,
    val homeName: String = "Home",
    val homeLat: String = "",
    val homeLon: String = "",
    val homeLocationError: String? = null,
    val homeRainStatusText: String? = null,
    val isCheckingHomeRain: Boolean = false
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val openMeteoApiService: OpenMeteoApiService? = null,
    private val locationProvider: LocationProvider? = null,
    private val appContext: Context? = null
) : ViewModel() {

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
                    else -> if (url.startsWith("http://192.168.") || (url.startsWith("http://10.") && url != SettingsStore.EMULATOR_BASE_URL)) ServerTarget.PHONE_LAN else ServerTarget.CUSTOM
                }
                _uiState.update {
                    it.copy(
                        serverTarget = target,
                        isEmulatorPreset = (url == SettingsStore.EMULATOR_BASE_URL),
                        customUrl = if (target == ServerTarget.CUSTOM) url else it.customUrl,
                        phoneLanUrl = if (target == ServerTarget.PHONE_LAN) url else it.phoneLanUrl
                    )
                }
            }
        }
        viewModelScope.launch { settingsStore.savedPhoneUrlFlow.collect { p -> _uiState.update { it.copy(phoneLanUrl = p) } } }
        viewModelScope.launch { settingsStore.manualLatFlow.collect { lat -> _uiState.update { it.copy(manualLat = lat.toString()) } } }
        viewModelScope.launch { settingsStore.manualLonFlow.collect { lon -> _uiState.update { it.copy(manualLon = lon.toString()) } } }
        viewModelScope.launch { settingsStore.schoolStartFlow.collect { s -> _uiState.update { it.copy(schoolStart = s) } } }
        viewModelScope.launch { settingsStore.schoolEndFlow.collect { e -> _uiState.update { it.copy(schoolEnd = e) } } }
        viewModelScope.launch { settingsStore.homeRainAlertEnabledFlow.collect { en -> _uiState.update { it.copy(homeRainAlertEnabled = en) } } }
        viewModelScope.launch { settingsStore.homeNameFlow.collect { n -> _uiState.update { it.copy(homeName = n) } } }
        viewModelScope.launch { settingsStore.homeLatFlow.collect { lat -> _uiState.update { it.copy(homeLat = lat?.toString() ?: it.homeLat) } } }
        viewModelScope.launch { settingsStore.homeLonFlow.collect { lon -> _uiState.update { it.copy(homeLon = lon?.toString() ?: it.homeLon) } } }
    }

    fun selectServerPreset(isEmulator: Boolean) = selectTarget(if (isEmulator) ServerTarget.EMULATOR else ServerTarget.CUSTOM)

    fun selectTarget(target: ServerTarget) {
        viewModelScope.launch {
            when (target) {
                ServerTarget.EMULATOR -> {
                    settingsStore.setBaseUrl(SettingsStore.EMULATOR_BASE_URL)
                    _uiState.update { it.copy(serverTarget = target, isEmulatorPreset = true, customUrlError = null, feedbackMessage = "Switched to Emulator") }
                }
                ServerTarget.USB_ADB -> {
                    settingsStore.setBaseUrl(SettingsStore.USB_ADB_BASE_URL)
                    _uiState.update { it.copy(serverTarget = target, isEmulatorPreset = false, customUrlError = null, feedbackMessage = "Switched to USB ADB") }
                }
                ServerTarget.PHONE_LAN -> {
                    val normalized = UrlUtils.normalizeBaseUrl(_uiState.value.phoneLanUrl) ?: SettingsStore.DEFAULT_PHONE_LAN_URL
                    settingsStore.setBaseUrl(normalized)
                    settingsStore.savePhoneUrl(normalized)
                    _uiState.update { it.copy(serverTarget = target, isEmulatorPreset = false, phoneLanUrl = normalized, feedbackMessage = "Switched to Phone LAN ($normalized)") }
                }
                ServerTarget.CUSTOM -> _uiState.update { it.copy(serverTarget = target, isEmulatorPreset = false) }
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
            _uiState.update { it.copy(latLonError = "Latitude (-90..90) & Longitude (-180..180) required") }
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

    fun onHomeRainAlertEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setHomeRainAlertEnabled(enabled)
            _uiState.update {
                it.copy(
                    homeRainAlertEnabled = enabled,
                    feedbackMessage = if (enabled) "Home rain alerts enabled" else "Home rain alerts disabled"
                )
            }
        }
    }

    fun onHomeNameChanged(name: String) = _uiState.update { it.copy(homeName = name) }
    fun onHomeLatChanged(lat: String) = _uiState.update { it.copy(homeLat = lat, homeLocationError = null) }
    fun onHomeLonChanged(lon: String) = _uiState.update { it.copy(homeLon = lon, homeLocationError = null) }

    fun saveHomeLocation() {
        val latStr = _uiState.value.homeLat.trim()
        val lonStr = _uiState.value.homeLon.trim()
        val name = _uiState.value.homeName.trim().ifBlank { "Home" }
        if (!Validators.isValidLatLon(latStr, lonStr)) {
            _uiState.update { it.copy(homeLocationError = "Valid latitude (-90..90) & longitude (-180..180) required") }
            return
        }
        val lat = latStr.toDouble()
        val lon = lonStr.toDouble()
        viewModelScope.launch {
            settingsStore.setHomeLocation(lat, lon, name)
            _uiState.update { it.copy(homeLocationError = null, feedbackMessage = "$name location saved ($lat, $lon)") }
        }
    }

    fun useCurrentLocationForHome() {
        viewModelScope.launch {
            val loc = try { locationProvider?.getCurrentLocation() } catch (_: Exception) { null }
            if (loc != null) {
                val latStr = String.format(java.util.Locale.US, "%.4f", loc.latitude)
                val lonStr = String.format(java.util.Locale.US, "%.4f", loc.longitude)
                val label = loc.label.ifBlank { "Home" }
                settingsStore.setHomeLocation(loc.latitude, loc.longitude, label)
                _uiState.update {
                    it.copy(homeLat = latStr, homeLon = lonStr, homeName = label, homeLocationError = null, feedbackMessage = "Set current GPS as Home ($label)")
                }
            } else {
                _uiState.update { it.copy(homeLocationError = "GPS unavailable. Enter coordinates manually.") }
            }
        }
    }

    fun checkHomeRainNow() {
        val lat = _uiState.value.homeLat.toDoubleOrNull()
        val lon = _uiState.value.homeLon.toDoubleOrNull()
        val name = _uiState.value.homeName.trim().ifBlank { "Home" }

        if (lat == null || lon == null || !Validators.isValidLatLon(_uiState.value.homeLat, _uiState.value.homeLon)) {
            _uiState.update { it.copy(homeLocationError = "Please set valid Home coordinates first") }
            return
        }

        val api = openMeteoApiService ?: return
        _uiState.update { it.copy(isCheckingHomeRain = true, homeRainStatusText = null) }

        viewModelScope.launch {
            val result = HomeRainChecker.checkRainAt(api, lat, lon)
            if (result != null) {
                val tempStr = result.temperatureCelsius?.let { "${it.toInt()}°C, " } ?: ""
                val text = if (result.isRaining) {
                    appContext?.let { ctx ->
                        AlertNotificationManager.postHomeRainAlert(ctx, name, result.description, force = true)
                    }
                    "🌧️ Raining at $name: ${result.description} ($tempStr notification posted!)"
                } else {
                    "☀️ Clear at $name: No rain ($tempStr${result.description})"
                }
                _uiState.update { it.copy(isCheckingHomeRain = false, homeRainStatusText = text) }
            } else {
                _uiState.update { it.copy(isCheckingHomeRain = false, homeRainStatusText = "Could not reach weather service. Please check connection.") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsStore.clearAll()
            _navigateAuth.tryEmit(Unit)
        }
    }
}
