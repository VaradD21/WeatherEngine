package com.weatherengine.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.weatherengine.app.core.notification.AlertNotificationManager

@Composable
fun TestNotificationCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun fireNotification(type: AlertNotificationManager.TestAlertType, label: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Toast.makeText(
                    context,
                    "Notification permission required. Please enable it in system settings.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }
        AlertNotificationManager.triggerTestNotification(context, type)
        Toast.makeText(context, "$label notification sent!", Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Test Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Tap any button to instantly simulate and preview high-priority weather alert notifications.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Button(
                onClick = { fireNotification(AlertNotificationManager.TestAlertType.HIGH_AQI, "High AQI") },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("😷 Trigger High AQI Alert (AQI 320)")
            }

            Button(
                onClick = { fireNotification(AlertNotificationManager.TestAlertType.HEAVY_RAIN, "Heavy Rain") },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("🌧️ Trigger Heavy Rain Alert (85%)")
            }

            Button(
                onClick = { fireNotification(AlertNotificationManager.TestAlertType.EXTREME_HEAT, "Extreme Heat") },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("🔥 Trigger Extreme Heat Alert (43°C)")
            }

            OutlinedButton(
                onClick = { fireNotification(AlertNotificationManager.TestAlertType.SEVERE_STORM, "Severe Storm") },
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("⚠️ Trigger Severe Storm Warning")
            }

            OutlinedButton(
                onClick = { fireNotification(AlertNotificationManager.TestAlertType.FROST_WARNING, "Frost Warning") },
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("❄️ Trigger Frost Warning (0°C)")
            }
        }
    }
}
