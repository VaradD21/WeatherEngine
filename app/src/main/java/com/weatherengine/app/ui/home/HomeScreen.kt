package com.weatherengine.app.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherengine.app.ui.home.components.HourlyForecastStrip
import com.weatherengine.app.ui.home.components.RenderPersonaWidget
import com.weatherengine.app.ui.theme.AmberWarning
import com.weatherengine.app.ui.theme.AuroraBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateSettings: () -> Unit,
    onNavigatePersonas: () -> Unit,
    onNavigateAuth: () -> Unit,
    onNavigateCustomize: () -> Unit = {}
) {
    val forecastState by viewModel.forecastState.collectAsState()
    val locationLabel by viewModel.locationLabel.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val selectedPersonas by viewModel.selectedPersonas.collectAsState()
    val activePersonaWidgets by viewModel.activePersonaWidgets.collectAsState()
    val homeUiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadData(isUserRefresh = true)
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(
                            text = "WeatherEngine",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = locationLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateCustomize) {
                        Icon(Icons.Default.Widgets, contentDescription = "Customize & Arrange Widgets")
                    }
                    IconButton(
                        onClick = { viewModel.loadData(isUserRefresh = true) },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh weather data")
                        }
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Open Settings")
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
            Column(modifier = Modifier.fillMaxSize()) {
                when (val fState = forecastState) {
                    is ForecastUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ForecastUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Unable to load forecast",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = fState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { viewModel.loadData(isUserRefresh = true) }) {
                                Text("Retry")
                            }
                        }
                    }
                    is ForecastUiState.Success -> {
                        val forecast = fState.forecast

                        if (forecast.isCached) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AmberWarning.copy(alpha = 0.8f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Showing cached forecast (${forecast.updatedAtLabel})",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(4.dp)) }

                            item(key = "current_conditions") {
                                CurrentConditionsCard(
                                    current = forecast.current,
                                    updatedAt = forecast.updatedAtLabel
                                )
                            }

                            item(key = "hourly_strip") {
                                HourlyForecastStrip(hours = forecast.hourly)
                            }

                            if (selectedPersonas.isEmpty()) {
                                item(key = "persona_prompt") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Choose what matters to you", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Tailor your weather dashboard with Health, Outdoor Fitness, or Commuter cards.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(onClick = onNavigatePersonas) { Text("Select Personas") }
                                        }
                                    }
                                }
                            } else {
                                item(key = "section_header") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("For You", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            val personaLabels = selectedPersonas.joinToString(" · ") { code ->
                                                when (code) {
                                                    "health_conscious" -> "Health"
                                                    "outdoor_fitness" -> "Fitness"
                                                    "commuter" -> "Commuter"
                                                    "beachgoer" -> "Beach & Surf"
                                                    "traveler" -> "Traveler"
                                                    "parent" -> "Parent & Family"
                                                    "gardener" -> "Gardener"
                                                    "event_planner" -> "Event Planner"
                                                    else -> code
                                                }
                                            }
                                            Text(personaLabels, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                        Row {
                                            IconButton(onClick = onNavigateCustomize) {
                                                Icon(Icons.Default.DashboardCustomize, contentDescription = "Arrange Widgets", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            TextButton(onClick = onNavigatePersonas) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Edit")
                                            }
                                        }
                                    }
                                }

                                items(activePersonaWidgets, key = { "persona_${it.name}" }) { widgetType ->
                                    RenderPersonaWidget(widgetType = widgetType, forecast = forecast)
                                }

                                if (homeUiState is HomeUiState.Content) {
                                    items((homeUiState as HomeUiState.Content).widgets, key = { "widget_${it.type}" }) { widget ->
                                        RenderDashboardWidget(widget = widget)
                                    }
                                }
                            }

                            item(key = "seven_day") {
                                SevenDayForecastCard(days = forecast.days)
                            }

                            item(key = "attribution") {
                                OpenMeteoAttributionCard()
                            }

                            if (!isAuthenticated) {
                                item(key = "personalize_banner") {
                                    PersonalizeBannerCard(onNavigateAuth = onNavigateAuth)
                                }
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}
