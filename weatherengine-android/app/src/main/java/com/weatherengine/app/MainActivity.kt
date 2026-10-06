package com.weatherengine.app

import android.app.Application
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
import com.weatherengine.app.data.di.AppContainer
import com.weatherengine.app.data.location.AndroidLocationProvider
import com.weatherengine.app.ui.auth.AuthScreen
import com.weatherengine.app.ui.auth.AuthViewModel
import com.weatherengine.app.ui.home.HomeScreen
import com.weatherengine.app.ui.home.HomeViewModel
import com.weatherengine.app.ui.personas.PersonaPickerScreen
import com.weatherengine.app.ui.personas.PersonaPickerViewModel
import com.weatherengine.app.ui.settings.SettingsScreen
import com.weatherengine.app.ui.settings.SettingsViewModel
import com.weatherengine.app.ui.theme.WeatherEngineTheme

class WeatherEngineApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object PersonaPicker : Screen("persona_picker")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherEngineTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WeatherEngineAppNav(app = this@MainActivity)
                }
            }
        }
    }
}

private inline fun <VM : ViewModel> vmFactory(crossinline create: () -> VM) =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }

@Composable
fun WeatherEngineAppNav(app: ComponentActivity) {
    val container = (app.application as WeatherEngineApp).appContainer
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        container.remoteRepository.sessionExpiredEvents.collect {
            Toast.makeText(app, "Session expired, please log in again", Toast.LENGTH_LONG).show()
            navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) {
            val vm: AuthViewModel = viewModel(factory = vmFactory { AuthViewModel(container.repository, container.settingsStore) })
            val goHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Auth.route) { inclusive = true } } }
            AuthScreen(viewModel = vm, onAuthSuccess = goHome, onSkip = goHome)
        }
        composable(Screen.Home.route) {
            val vm: HomeViewModel = viewModel(factory = vmFactory {
                HomeViewModel(container.forecastRepository, container.repository, container.settingsStore, AndroidLocationProvider(app))
            })
            HomeScreen(
                viewModel = vm,
                onNavigateSettings = { navController.navigate(Screen.Settings.route) },
                onNavigatePersonas = { navController.navigate(Screen.PersonaPicker.route) },
                onNavigateAuth = { navController.navigate(Screen.Auth.route) }
            )
        }
        composable(Screen.PersonaPicker.route) {
            val vm: PersonaPickerViewModel = viewModel(factory = vmFactory {
                PersonaPickerViewModel(container.settingsStore, repository = container.repository)
            })
            PersonaPickerScreen(
                viewModel = vm,
                onSaved = { navController.navigate(Screen.Home.route) { popUpTo(Screen.PersonaPicker.route) { inclusive = true } } }
            )
        }
        composable(Screen.Settings.route) {
            val vm: SettingsViewModel = viewModel(factory = vmFactory { SettingsViewModel(container.settingsStore) })
            SettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigatePersonas = { navController.navigate(Screen.PersonaPicker.route) },
                onLogout = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } }
            )
        }
    }
}
