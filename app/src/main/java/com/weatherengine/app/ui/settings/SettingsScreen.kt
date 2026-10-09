package com.weatherengine.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.core.UrlUtils
import com.weatherengine.app.core.Validators
import com.weatherengine.app.data.local.SettingsStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isEmulatorPreset: Boolean = true,
    val customUrl: String = "",
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
                val isEmulator = url == SettingsStore.DEFAULT_BASE_URL
                _uiState.update { it.copy(isEmulatorPreset = isEmulator, customUrl = if (!isEmulator) url else it.customUrl) }
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
        viewModelScope.launch {
            if (isEmulator) {
                settingsStore.setBaseUrl(SettingsStore.DEFAULT_BASE_URL)
                _uiState.update { it.copy(isEmulatorPreset = true, customUrlError = null) }
            } else {
                _uiState.update { it.copy(isEmulatorPreset = false) }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigatePersonas: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateAuth.collect { onLogout() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            state.feedbackMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Backend Server Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth().clickable { viewModel.selectServerPreset(true) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.isEmulatorPreset, onClick = { viewModel.selectServerPreset(true) })
                        Text("Android Emulator host (10.0.2.2:8080)", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(Modifier.fillMaxWidth().clickable { viewModel.selectServerPreset(false) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !state.isEmulatorPreset, onClick = { viewModel.selectServerPreset(false) })
                        Text("Custom Server URL", modifier = Modifier.padding(start = 8.dp))
                    }

                    if (!state.isEmulatorPreset) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.customUrl,
                            onValueChange = viewModel::onCustomUrlChanged,
                            label = { Text("Server URL (http:// or https://)") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.customUrlError != null,
                            supportingText = { state.customUrlError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = viewModel::saveCustomUrl) { Text("Apply URL") }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Manual Location Coordinates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Fallback when GPS is disabled or unavailable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = state.manualLat, onValueChange = viewModel::onManualLatChanged, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = state.manualLon, onValueChange = viewModel::onManualLonChanged, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                    }
                    state.latLonError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::saveManualCoordinates) { Text("Save Coordinates") }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("School Hours (Parent & Family)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Used for morning and afternoon school commute weather verdicts. Assumes Monday-Friday school days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.schoolStart,
                            onValueChange = viewModel::onSchoolStartChanged,
                            label = { Text("Start (HH:mm)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = state.schoolEnd,
                            onValueChange = viewModel::onSchoolEndChanged,
                            label = { Text("End (HH:mm)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.schoolHoursError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::saveSchoolHours,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Save School Hours")
                    }
                }
            }

            OutlinedButton(onClick = onNavigatePersonas, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Change Personas")
            }

            HorizontalDivider()

            Button(
                onClick = viewModel::logout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Log Out")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
