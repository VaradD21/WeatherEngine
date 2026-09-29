package com.weatherengine.app.ui.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object PersonaPicker : Screen("persona_picker")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
}
