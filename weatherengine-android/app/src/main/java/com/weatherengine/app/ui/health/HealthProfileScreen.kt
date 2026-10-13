package com.weatherengine.app.ui.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthProfileScreen(
    viewModel: HealthProfileViewModel,
    onComplete: () -> Unit,
    onSkip: () -> Unit = onComplete
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.savedSuccess.collect { onComplete() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Details", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "Personal & Health Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Step 2 of 2: Customize health conditions, alert thresholds, and your favorite weather features so your dashboard is tailored specifically to you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Section 1: Health Conditions & Sensitivities
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Health Conditions & Weather Sensitivity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    HealthCheckRow("Asthma & Respiratory Sensitivities", state.hasAsthma, viewModel::toggleAsthma)
                    HealthCheckRow("Seasonal Allergies & Hay Fever", state.hasAllergies, viewModel::toggleAllergies)
                    HealthCheckRow("Sensitive Skin & Sun Sensitivity", state.hasSkinSensitivity, viewModel::toggleSkinSensitivity)
                    HealthCheckRow("Weather Migraines & Pressure Changes", state.hasMigraines, viewModel::toggleMigraines)
                    HealthCheckRow("Joint Pain & Arthritis (Cold / Damp Weather)", state.hasJointPain, viewModel::toggleJointPain)
                    HealthCheckRow("Heat & Cardiovascular Sensitivity", state.hasHeatSensitivity, viewModel::toggleHeatSensitivity)
                }
            }

            // Section 2: Project Focus & Personas
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("What do you use WeatherEngine for?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Select any that apply to enable personalized forecast widgets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    viewModel.availablePersonas.forEach { persona ->
                        val isSelected = state.selectedPersonas.contains(persona.code)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.togglePersona(persona.code) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(persona.title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                Text(persona.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Section 3: Alert Thresholds
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Alert Trigger Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Set custom thresholds for timely notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Text("Air Quality Alert: AQI > ${state.aqiThreshold}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = state.aqiThreshold.toFloat(),
                        onValueChange = { viewModel.setAqiThreshold(it.roundToInt()) },
                        valueRange = 50f..200f,
                        steps = 14
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("UV Alert: Index > ${state.uvThreshold}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = state.uvThreshold.toFloat(),
                        onValueChange = { viewModel.setUvThreshold(it.roundToInt()) },
                        valueRange = 3f..11f,
                        steps = 7
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("High Humidity Alert: > ${state.humidityThreshold}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = state.humidityThreshold.toFloat(),
                        onValueChange = { viewModel.setHumidityThreshold(it.roundToInt()) },
                        valueRange = 50f..90f,
                        steps = 7
                    )
                }
            }

            // Section 4: Notifications Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Weather Alerts & Notifications", fontWeight = FontWeight.SemiBold)
                        Text("Deliver notifications when weather limits are reached", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.alertsEnabled, onCheckedChange = viewModel::toggleAlertsEnabled)
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            // Save Button
            Button(
                onClick = viewModel::saveProfile,
                enabled = !state.isLoading,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save & Go to Dashboard", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HealthCheckRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}
