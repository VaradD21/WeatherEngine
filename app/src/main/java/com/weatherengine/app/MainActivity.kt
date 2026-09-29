package com.weatherengine.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.weatherengine.app.data.location.AndroidLocationProvider
import com.weatherengine.app.ui.auth.AuthScreen
import com.weatherengine.app.ui.auth.AuthViewModel
import com.weatherengine.app.ui.home.HomeScreen
import com.weatherengine.app.ui.home.HomeViewModel
import com.weatherengine.app.ui.navigation.Screen
import com.weatherengine.app.ui.personas.PersonaPickerScreen
import com.weatherengine.app.ui.personas.PersonaPickerViewModel
import com.weatherengine.app.ui.settings.SettingsScreen
import com.weatherengine.app.ui.settings.SettingsViewModel
import com.weatherengine.app.ui.theme.WeatherEngineTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherEngineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherEngineAppNav(app = this@MainActivity)
                }
            }
        }
    }
}

@Composable
fun WeatherEngineAppNav(app: ComponentActivity) {
    val appContainer = (app.application as WeatherEngineApp).appContainer
    val navController = rememberNavController()
    val startDestination = Screen.Home.route

    LaunchedEffect(Unit) {
        appContainer.remoteRepository.sessionExpiredEvents.collect {
            Toast.makeText(app, "Session expired, please log in again", Toast.LENGTH_LONG).show()
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            val authVm: AuthViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(
                            repository = appContainer.repository,
                            settingsStore = appContainer.settingsStore
                        ) as T
                    }
                }
            )
            AuthScreen(
                viewModel = authVm,
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeVm: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(
                            forecastRepository = appContainer.forecastRepository,
                            repository = appContainer.repository,
                            settingsStore = appContainer.settingsStore,
                            locationProvider = AndroidLocationProvider(app)
                        ) as T
                    }
                }
            )
            HomeScreen(
                viewModel = homeVm,
                onNavigateSettings = { navController.navigate(Screen.Settings.route) },
                onNavigatePersonas = { navController.navigate(Screen.PersonaPicker.route) },
                onNavigateAuth = { navController.navigate(Screen.Auth.route) }
            )
        }

        composable(Screen.PersonaPicker.route) {
            val pickerVm: PersonaPickerViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return PersonaPickerViewModel(
                            settingsStore = appContainer.settingsStore
                        ) as T
                    }
                }
            )
            PersonaPickerScreen(
                viewModel = pickerVm,
                onSaved = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PersonaPicker.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            val settingsVm: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SettingsViewModel(
                            settingsStore = appContainer.settingsStore
                        ) as T
                    }
                }
            )
            SettingsScreen(
                viewModel = settingsVm,
                onNavigateBack = { navController.popBackStack() },
                onNavigatePersonas = { navController.navigate(Screen.PersonaPicker.route) },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
