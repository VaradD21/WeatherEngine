package com.weatherengine.app.ui.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                title = { Text("Health Profile", fontWeight = FontWeight.Bold) },
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
                text = "Health & Sensitivity Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Highlight conditions and customize your alert thresholds for AQI, UV radiation, pollen, and humidity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Section 1: Conditions & Sensitivities
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your Health Conditions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.hasAsthma,
                            onCheckedChange = viewModel::toggleAsthma
                        )
                        Text("Asthma & Respiratory Sensitivity", modifier = Modifier.padding(start = 8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.hasAllergies,
                            onCheckedChange = viewModel::toggleAllergies
                        )
                        Text("Seasonal Allergies & Hay Fever", modifier = Modifier.padding(start = 8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.hasSkinSensitivity,
                            onCheckedChange = viewModel::toggleSkinSensitivity
                        )
                        Text("Sensitive Skin, Eczema & Sun Sensitivity", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            // Section 2: Alert Thresholds
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Alert Trigger Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "You will receive an alert whenever conditions exceed these values.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    // AQI Slider
                    Text(
                        text = "Air Quality Alert: AQI > ${state.aqiThreshold}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = state.aqiThreshold.toFloat(),
                        onValueChange = { viewModel.setAqiThreshold(it.roundToInt()) },
                        valueRange = 50f..200f,
                        steps = 14
                    )

                    Spacer(Modifier.height(12.dp))

                    // UV Slider
                    Text(
                        text = "UV Alert: Index > ${state.uvThreshold}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = state.uvThreshold.toFloat(),
                        onValueChange = { viewModel.setUvThreshold(it.roundToInt()) },
                        valueRange = 3f..11f,
                        steps = 7
                    )

                    Spacer(Modifier.height(12.dp))

                    // Humidity Slider
                    Text(
                        text = "High Humidity Alert: > ${state.humidityThreshold}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = state.humidityThreshold.toFloat(),
                        onValueChange = { viewModel.setHumidityThreshold(it.roundToInt()) },
                        valueRange = 50f..90f,
                        steps = 7
                    )
                }
            }

            // Section 3: Notification Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Phone Alert Notifications", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Deliver push alerts when limits are breached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.alertsEnabled,
                        onCheckedChange = viewModel::toggleAlertsEnabled
                    )
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            // Action Buttons
            Button(
                onClick = viewModel::saveProfile,
                enabled = !state.isLoading,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save & Continue", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
