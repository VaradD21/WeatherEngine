package com.weatherengine.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.weatherengine.app.core.WidgetUi

@Composable
fun RenderDashboardWidget(
    widget: WidgetUi,
    modifier: Modifier = Modifier
) {
    when (widget) {
        is WidgetUi.SchoolCommute -> SchoolCommuteCard(commute = widget, modifier = modifier)
        is WidgetUi.RainAlert -> RainAlertCard(rain = widget, modifier = modifier)
        is WidgetUi.SevereWeather -> SevereWeatherCard(severe = widget, modifier = modifier)
        is WidgetUi.Pollen -> PollenCard(pollen = widget, modifier = modifier)
        is WidgetUi.Aqi -> AqiCard(aqi = widget, modifier = modifier)
        is WidgetUi.SunriseSunset -> SunriseSunsetCard(sun = widget, modifier = modifier)
        is WidgetUi.BestRunning -> BestRunningHoursCard(running = widget, modifier = modifier)
        is WidgetUi.Wind -> WindSpeedCard(wind = widget, modifier = modifier)
        is WidgetUi.HeatAlert -> HeatAlertCard(heat = widget, modifier = modifier)
        is WidgetUi.WaveHeight -> WaveHeightCard(wave = widget, modifier = modifier)
        is WidgetUi.SeaTemp -> SeaTempCard(sea = widget, modifier = modifier)
        is WidgetUi.Tide -> TideCard(tide = widget, modifier = modifier)
        is WidgetUi.SavedDestinations -> SavedDestinationsCard(saved = widget, modifier = modifier)
        is WidgetUi.FlightAlert -> FlightAlertCard(flight = widget, modifier = modifier)
        is WidgetUi.PackingSuggestion -> PackingSuggestionCard(packing = widget, modifier = modifier)
        // Gardener
        is WidgetUi.SoilMoisture -> SoilMoistureCard(soil = widget, modifier = modifier)
        is WidgetUi.RainfallPrediction -> RainfallPredictionCard(rainfall = widget, modifier = modifier)
        is WidgetUi.FrostAlert -> FrostAlertCard(frost = widget, modifier = modifier)
        is WidgetUi.PlantingGuidance -> PlantingGuidanceCard(planting = widget, modifier = modifier)
        // Event Planner
        is WidgetUi.ExtendedForecast -> ExtendedForecastCard(forecast = widget, modifier = modifier)
        is WidgetUi.RainProbability -> RainProbabilityCard(rain = widget, modifier = modifier)
        is WidgetUi.ComfortIndex -> ComfortIndexCard(comfort = widget, modifier = modifier)
        is WidgetUi.StatusOnly -> StatusOnlyCard(widget = widget, modifier = modifier)
        else -> {}
    }
}
