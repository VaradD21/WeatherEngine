package com.weatherengine.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ServerTarget {
    EMULATOR,
    PHONE_LAN,
    USB_ADB,
    CUSTOM
}

@Composable
fun ServerTargetCard(
    state: SettingsUiState,
    onSelectTarget: (ServerTarget) -> Unit,
    onPhoneUrlChanged: (String) -> Unit,
    onSavePhoneUrl: () -> Unit,
    onCustomUrlChanged: (String) -> Unit,
    onSaveCustomUrl: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Backend Server Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Quick switch between Emulator, Phone Wi-Fi, or USB cable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // 1. Android Emulator
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTarget(ServerTarget.EMULATOR) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.serverTarget == ServerTarget.EMULATOR,
                    onClick = { onSelectTarget(ServerTarget.EMULATOR) }
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text("Android Emulator", fontWeight = FontWeight.Medium)
                    Text("10.0.2.2:8080", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(6.dp))

            // 2. Physical Phone (Local Wi-Fi)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTarget(ServerTarget.PHONE_LAN) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.serverTarget == ServerTarget.PHONE_LAN,
                    onClick = { onSelectTarget(ServerTarget.PHONE_LAN) }
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text("Physical Phone (Wi-Fi LAN)", fontWeight = FontWeight.Medium)
                    Text(
                        if (state.phoneLanUrl.isNotBlank()) state.phoneLanUrl else "PC Local IP:8080",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.serverTarget == ServerTarget.PHONE_LAN) {
                Column(Modifier.padding(start = 32.dp, top = 4.dp, bottom = 4.dp)) {
                    OutlinedTextField(
                        value = state.phoneLanUrl,
                        onValueChange = onPhoneUrlChanged,
                        label = { Text("PC Wi-Fi URL (http://<IP>:8080)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = onSavePhoneUrl) {
                        Text("Apply Phone URL")
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 3. USB ADB Reverse
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTarget(ServerTarget.USB_ADB) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.serverTarget == ServerTarget.USB_ADB,
                    onClick = { onSelectTarget(ServerTarget.USB_ADB) }
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text("USB ADB Reverse (localhost:8080)", fontWeight = FontWeight.Medium)
                    Text("Requires: adb reverse tcp:8080 tcp:8080", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(6.dp))

            // 4. Custom Server URL
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTarget(ServerTarget.CUSTOM) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.serverTarget == ServerTarget.CUSTOM,
                    onClick = { onSelectTarget(ServerTarget.CUSTOM) }
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text("Custom Server URL", fontWeight = FontWeight.Medium)
                }
            }

            if (state.serverTarget == ServerTarget.CUSTOM) {
                Column(Modifier.padding(start = 32.dp, top = 4.dp, bottom = 4.dp)) {
                    OutlinedTextField(
                        value = state.customUrl,
                        onValueChange = onCustomUrlChanged,
                        label = { Text("Server URL (http:// or https://)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.customUrlError != null,
                        supportingText = { state.customUrlError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = onSaveCustomUrl) {
                        Text("Apply Custom URL")
                    }
                }
            }
        }
    }
}
