package com.weatherengine.app.ui.customize

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherengine.app.ui.theme.AuroraBackground
import com.weatherengine.app.ui.theme.AuroraCyan
import com.weatherengine.app.ui.theme.GlassBadge
import com.weatherengine.app.ui.theme.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeWidgetsScreen(
    viewModel: CustomizeWidgetsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Customize Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::resetToDefault) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset to Default")
                    }
                }
            )
        }
    ) { innerPadding ->
        AuroraBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // ── Home Screen Widget Promo Card ──────────────────────────────
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = AuroraCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Home Screen Widget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Place a live WeatherEngine glass widget on your phone's home screen to see temperature, AQI, and alerts without opening the app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.pinAppWidget(context) },
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Widget to Home Screen")
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Dashboard Reordering",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use arrows to position what widget goes where on your main feed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Reorderable Widget Items ──────────────────────────────────
                itemsIndexed(state.items, key = { _, item -> item.type }) { index, item ->
                    WidgetItemCard(
                        item = item,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.items.lastIndex,
                        onMoveUp = { viewModel.moveUp(index) },
                        onMoveDown = { viewModel.moveDown(index) },
                        onToggle = { enabled -> viewModel.toggleVisibility(item.type, enabled) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun WidgetItemCard(
    item: CustomWidgetItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassBadge(
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Up / Down arrows
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}
