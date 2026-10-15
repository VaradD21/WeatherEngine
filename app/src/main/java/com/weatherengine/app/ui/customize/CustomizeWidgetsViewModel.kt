package com.weatherengine.app.ui.customize

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.widget.WeatherAppWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CustomWidgetItem(
    val type: String,
    val title: String,
    val description: String,
    val category: String,
    val isEnabled: Boolean = true
)

data class CustomizeWidgetsUiState(
    val items: List<CustomWidgetItem> = emptyList(),
    val isSaved: Boolean = false,
    val feedbackMessage: String? = null
)

class CustomizeWidgetsViewModel(
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomizeWidgetsUiState())
    val uiState: StateFlow<CustomizeWidgetsUiState> = _uiState.asStateFlow()

    init {
        loadWidgetConfiguration()
    }

    private fun loadWidgetConfiguration() {
        viewModelScope.launch {
            val customOrder = settingsStore.widgetOrderFlow.first()
            val disabledSet = settingsStore.disabledWidgetsFlow.first()

            val allDefaults = defaultWidgetRegistry()

            // Merge order: custom ordered items first, then any remaining items
            val orderedTypes = if (customOrder.isNotEmpty()) {
                val registeredTypes = allDefaults.map { it.type }
                val validCustom = customOrder.filter { it in registeredTypes }
                val remaining = registeredTypes.filter { it !in validCustom }
                validCustom + remaining
            } else {
                allDefaults.map { it.type }
            }

            val mapByType = allDefaults.associateBy { it.type }
            val resolvedList = orderedTypes.mapNotNull { type ->
                mapByType[type]?.copy(isEnabled = type !in disabledSet)
            }

            _uiState.value = CustomizeWidgetsUiState(items = resolvedList)
        }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        val list = _uiState.value.items.toMutableList()
        val item = list.removeAt(index)
        list.add(index - 1, item)
        _uiState.value = _uiState.value.copy(items = list)
        saveOrder(list)
    }

    fun moveDown(index: Int) {
        val list = _uiState.value.items.toMutableList()
        if (index >= list.lastIndex) return
        val item = list.removeAt(index)
        list.add(index + 1, item)
        _uiState.value = _uiState.value.copy(items = list)
        saveOrder(list)
    }

    fun toggleVisibility(widgetType: String, enabled: Boolean) {
        val list = _uiState.value.items.map {
            if (it.type == widgetType) it.copy(isEnabled = enabled) else it
        }
        _uiState.value = _uiState.value.copy(items = list)
        viewModelScope.launch {
            settingsStore.setWidgetDisabled(widgetType, !enabled)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            settingsStore.saveWidgetOrder(emptyList())
            val all = defaultWidgetRegistry()
            _uiState.value = CustomizeWidgetsUiState(
                items = all,
                feedbackMessage = "Widget arrangement reset to default"
            )
        }
    }

    fun pinAppWidget(context: Context): Boolean {
        val success = WeatherAppWidgetProvider.pinToHomeScreen(context)
        _uiState.value = _uiState.value.copy(
            feedbackMessage = if (success) "Home screen widget prompt opened!" else "Pinning not supported on this launcher"
        )
        return success
    }

    private fun saveOrder(items: List<CustomWidgetItem>) {
        viewModelScope.launch {
            settingsStore.saveWidgetOrder(items.map { it.type })
        }
    }

    companion object {
        fun defaultWidgetRegistry() = listOf(
            CustomWidgetItem("school_commute_card", "School Commute", "Morning & afternoon school run verdicts", "Family"),
            CustomWidgetItem("rain_alert_card", "Rain Alerts", "Precipitation timing & intensity warnings", "Family"),
            CustomWidgetItem("severe_weather_card", "Severe Weather Warnings", "Critical weather advisories", "Family"),
            CustomWidgetItem("aqi_card", "Air Quality (US AQI)", "Real-time PM2.5 & Air Quality rating", "Health"),
            CustomWidgetItem("pollen_card", "Pollen Levels", "Pollen grains count & allergens", "Health"),
            CustomWidgetItem("uv_index_card", "UV Radiation Index", "Peak UV index & sun exposure guidelines", "Health"),
            CustomWidgetItem("humidity_card", "Humidity & Discomfort", "Relative humidity & dew point", "Health"),
            CustomWidgetItem("sunrise_sunset_card", "Sunrise & Sunset", "Sun transit & daylight hours remaining", "Fitness"),
            CustomWidgetItem("best_running_hours_card", "Best Running Hours", "Optimal outdoor workout windows", "Fitness"),
            CustomWidgetItem("wind_speed_card", "Wind Speed & Gusts", "Velocity, Beaufort scale & gale warnings", "Fitness"),
            CustomWidgetItem("heat_alert_card", "Heat Alerts & Index", "Heatwave warnings & apparent temperature", "Fitness"),
            CustomWidgetItem("traffic_card", "Commute Traffic Impact", "Weather-induced commute disruptions", "Commuter"),
            CustomWidgetItem("visibility_card", "Road Visibility", "Fog, mist, and driving hazards", "Commuter"),
            CustomWidgetItem("storm_fog_alert_card", "Storm & Fog Advisory", "Travel-impacting weather warnings", "Commuter"),
            CustomWidgetItem("wave_height_card", "Wave Height & Sea State", "Surf swell and marine safety", "Beach"),
            CustomWidgetItem("sea_temp_card", "Sea Temperature", "Water warmth for swimming & watersports", "Beach"),
            CustomWidgetItem("tide_card", "Tide Timings", "High & low tide prediction", "Beach"),
            CustomWidgetItem("saved_destinations_card", "Saved Destinations", "Quick weather check for travel spots", "Travel"),
            CustomWidgetItem("flight_alert_card", "Flight Weather Alerts", "Airport weather disruptions", "Travel"),
            CustomWidgetItem("packing_suggestion_card", "Smart Packing Advice", "Clothing & gear recommendations", "Travel"),
            CustomWidgetItem("soil_moisture_card", "Soil Moisture", "Topsoil moisture levels for crops & plants", "Gardener"),
            CustomWidgetItem("rainfall_prediction_card", "Rainfall Prediction", "7-day rain accumulation forecast", "Gardener"),
            CustomWidgetItem("frost_alert_card", "Frost & Freeze Warning", "Low temperature frost defense alerts", "Gardener"),
            CustomWidgetItem("planting_guidance_card", "Seasonal Planting", "Sowing & harvesting recommendations", "Gardener"),
            CustomWidgetItem("extended_forecast_card", "Extended 14-Day Outlook", "Long-range planning for outdoor events", "Events"),
            CustomWidgetItem("rain_probability_card", "Rain Probability Table", "Hourly rain chances for event times", "Events"),
            CustomWidgetItem("comfort_index_card", "Event Comfort Index", "Temperature & humidity outdoor score", "Events")
        )
    }
}
