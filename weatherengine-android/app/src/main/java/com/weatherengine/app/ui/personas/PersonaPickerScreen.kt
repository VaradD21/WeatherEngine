package com.weatherengine.app.ui.personas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.repository.WeatherEngineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PersonaOption(val code: String, val title: String, val description: String)

data class PersonaPickerUiState(
    val selectedCodes: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class PersonaPickerViewModel(
    private val settingsStore: SettingsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val repository: WeatherEngineRepository? = null
) : ViewModel() {

    val availablePersonas = listOf(
        PersonaOption("health_conscious", "Health-conscious", "AQI, UV index, and heat alerts tailored for health and respiratory safety."),
        PersonaOption("outdoor_fitness", "Outdoor Fitness", "Wind speed, storm/fog alerts, humidity, and UV for sports and workouts."),
        PersonaOption("commuter", "Commuter", "Road visibility, commute traffic updates, and sudden transit alerts.")
    )

    private val _uiState = MutableStateFlow(PersonaPickerUiState())
    val uiState: StateFlow<PersonaPickerUiState> = _uiState.asStateFlow()

    private val _navigateHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateHome: SharedFlow<Unit> = _navigateHome.asSharedFlow()

    init {
        loadCurrentPersonas()
    }

    fun loadCurrentPersonas() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            var saved = settingsStore.selectedPersonasFlow.first()
            val token = settingsStore.tokenFlow.first()
            if (repository != null && !token.isNullOrBlank()) {
                val res = repository.getPersonas()
                if (res is NetworkResult.Success && res.data.isNotEmpty()) {
                    saved = res.data.map { it.code }.toSet()
                    settingsStore.saveSelectedPersonas(saved)
                }
            }
            _uiState.update { it.copy(isLoading = false, selectedCodes = saved) }
        }
    }

    fun toggleSelection(code: String) {
        _uiState.update { state ->
            val updated = state.selectedCodes.toMutableSet()
            if (!updated.remove(code)) updated.add(code)
            state.copy(selectedCodes = updated, errorMessage = null)
        }
    }

    fun save() {
        val selected = _uiState.value.selectedCodes
        if (selected.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one persona") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val token = settingsStore.tokenFlow.first()
                if (repository != null && !token.isNullOrBlank()) {
                    when (val res = repository.setPersonas(selected.toList())) {
                        is NetworkResult.Success -> {
                            settingsStore.saveSelectedPersonas(res.data.map { it.code }.toSet().ifEmpty { selected })
                            _uiState.update { it.copy(isSaving = false) }
                            _navigateHome.tryEmit(Unit)
                            return@launch
                        }
                        is NetworkResult.Failure -> {
                            _uiState.update { it.copy(isSaving = false, errorMessage = res.message) }
                            return@launch
                        }
                    }
                }
                settingsStore.saveSelectedPersonas(selected)
                _uiState.update { it.copy(isSaving = false) }
                _navigateHome.tryEmit(Unit)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.localizedMessage ?: "Failed to save personas") }
            }
        }
    }
}

@Composable
fun PersonaPickerScreen(viewModel: PersonaPickerViewModel, onSaved: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateHome.collect { onSaved() }
    }

    Scaffold(
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving && !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save Personas")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Customize Your Experience", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Select one or more personas to customize which weather insights you see.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                items(viewModel.availablePersonas) { persona ->
                    val isSelected = state.selectedCodes.contains(persona.code)
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleSelection(persona.code) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(persona.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(persona.description, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = if (isSelected) "Selected" else "Not selected",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(28.dp).padding(start = 8.dp)
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
