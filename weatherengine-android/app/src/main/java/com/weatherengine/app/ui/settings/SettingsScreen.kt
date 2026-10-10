package com.weatherengine.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            ServerTargetCard(
                state = state,
                onSelectTarget = viewModel::selectTarget,
                onPhoneUrlChanged = viewModel::onPhoneUrlChanged,
                onSavePhoneUrl = viewModel::savePhoneUrl,
                onCustomUrlChanged = viewModel::onCustomUrlChanged,
                onSaveCustomUrl = viewModel::saveCustomUrl
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Manual Location Coordinates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Fallback when GPS is disabled or unavailable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.manualLat,
                            onValueChange = viewModel::onManualLatChanged,
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = state.manualLon,
                            onValueChange = viewModel::onManualLonChanged,
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.latLonError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::saveManualCoordinates) {
                        Text("Save Coordinates")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("School Hours (Parent & Family)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Used for morning and afternoon school commute weather verdicts. Assumes Monday-Friday school days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
