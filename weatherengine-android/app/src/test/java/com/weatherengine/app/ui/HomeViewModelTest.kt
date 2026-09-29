package com.weatherengine.app.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.weatherengine.app.core.forecast.CurrentWeatherUi
import com.weatherengine.app.core.forecast.ForecastUi
import com.weatherengine.app.data.api.FailureKind
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.model.HomepageResponse
import com.weatherengine.app.data.model.WidgetDto
import com.weatherengine.app.data.repository.ForecastRepository
import com.weatherengine.app.data.repository.WeatherEngineRepository
import com.weatherengine.app.ui.home.ForecastUiState
import com.weatherengine.app.ui.home.HomeUiState
import com.weatherengine.app.ui.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsStore: SettingsStore

    private val sampleForecast = ForecastUi(
        locationLabel = "Mumbai",
        updatedAtLabel = "Updated just now",
        timestampMs = System.currentTimeMillis(),
        current = CurrentWeatherUi(
            temperatureFormatted = "31°",
            apparentTemperatureFormatted = "Feels like 38°",
            condition = "Sunny",
            iconKey = "clear_day",
            humidityFormatted = "74%",
            windFormatted = "12.5 km/h",
            precipitationFormatted = "0.0 mm"
        ),
        days = emptyList()
    )

    private val fakeForecastRepo = object : ForecastRepository {
        var shouldSucceed = true
        override suspend fun getForecast(
            lat: Double,
            lon: Double,
            locationLabel: String,
            forceRefresh: Boolean
        ): NetworkResult<ForecastUi> {
            return if (shouldSucceed) {
                NetworkResult.Success(sampleForecast.copy(locationLabel = locationLabel))
            } else {
                NetworkResult.Failure(FailureKind.Network, "Network connection issue")
            }
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val file = File(tempFolder.root, "test_home_vm.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(testDispatcher),
            produceFile = { file }
        )
        settingsStore = SettingsStore(dataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testForecast_Success() = runTest(testDispatcher) {
        fakeForecastRepo.shouldSucceed = true
        val fakeEngineRepo = object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String) = error("unused")
            override suspend fun login(email: String, password: String) = error("unused")
            override suspend fun getPersonas() = error("unused")
            override suspend fun setPersonas(codes: List<String>) = error("unused")
            override suspend fun getHomepage(lat: Double, lon: Double) = error("unused")
        }

        val viewModel = HomeViewModel(fakeForecastRepo, fakeEngineRepo, settingsStore, null, testDispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.forecastState.value is ForecastUiState.Success)
        val state = viewModel.forecastState.value as ForecastUiState.Success
        assertEquals("31°", state.forecast.current.temperatureFormatted)
    }

    @Test
    fun testForecast_Error() = runTest(testDispatcher) {
        fakeForecastRepo.shouldSucceed = false
        val fakeEngineRepo = object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String) = error("unused")
            override suspend fun login(email: String, password: String) = error("unused")
            override suspend fun getPersonas() = error("unused")
            override suspend fun setPersonas(codes: List<String>) = error("unused")
            override suspend fun getHomepage(lat: Double, lon: Double) = error("unused")
        }

        val viewModel = HomeViewModel(fakeForecastRepo, fakeEngineRepo, settingsStore, null, testDispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.forecastState.value is ForecastUiState.Error)
        val err = viewModel.forecastState.value as ForecastUiState.Error
        assertEquals("Network connection issue", err.message)
    }

    @Test
    fun testHomepage_GuestMode_WhenUnauthenticated() = runTest(testDispatcher) {
        fakeForecastRepo.shouldSucceed = true
        val fakeEngineRepo = object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String) = error("unused")
            override suspend fun login(email: String, password: String) = error("unused")
            override suspend fun getPersonas() = error("unused")
            override suspend fun setPersonas(codes: List<String>) = error("unused")
            override suspend fun getHomepage(lat: Double, lon: Double) = error("unused")
        }

        val viewModel = HomeViewModel(fakeForecastRepo, fakeEngineRepo, settingsStore, null, testDispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is HomeUiState.Guest)
    }

    @Test
    fun testHomepage_Content_WhenAuthenticated() = runTest(testDispatcher) {
        settingsStore.saveAuth("test-token", "user@test.com")
        val widgetJson = buildJsonObject { put("aqi", 2); put("category", "Fair") }
        val fakeEngineRepo = object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String) = error("unused")
            override suspend fun login(email: String, password: String) = error("unused")
            override suspend fun getPersonas() = error("unused")
            override suspend fun setPersonas(codes: List<String>) = error("unused")
            override suspend fun getHomepage(lat: Double, lon: Double): NetworkResult<HomepageResponse> {
                return NetworkResult.Success(
                    HomepageResponse(widgets = listOf(WidgetDto("aqi_card", widgetJson)))
                )
            }
        }

        val viewModel = HomeViewModel(fakeForecastRepo, fakeEngineRepo, settingsStore, null, testDispatcher)
        advanceUntilIdle()

        if (HomeViewModel.USE_BACKEND_WIDGETS) {
            assertTrue(viewModel.uiState.value is HomeUiState.Content)
            val content = viewModel.uiState.value as HomeUiState.Content
            assertEquals(1, content.widgets.size)
        } else {
            assertTrue(viewModel.uiState.value is HomeUiState.Guest)
        }
    }

    @Test
    fun testPersonaSelectionChange_UpdatesWidgetsWithoutRefetchingForecast() = runTest(testDispatcher) {
        var forecastFetchCount = 0
        val countingForecastRepo = object : ForecastRepository {
            override suspend fun getForecast(
                lat: Double,
                lon: Double,
                locationLabel: String,
                forceRefresh: Boolean
            ): NetworkResult<ForecastUi> {
                forecastFetchCount++
                return NetworkResult.Success(sampleForecast)
            }
        }
        val fakeEngineRepo = object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String) = error("unused")
            override suspend fun login(email: String, password: String) = error("unused")
            override suspend fun getPersonas() = error("unused")
            override suspend fun setPersonas(codes: List<String>) = error("unused")
            override suspend fun getHomepage(lat: Double, lon: Double) = error("unused")
        }

        val viewModel = HomeViewModel(countingForecastRepo, fakeEngineRepo, settingsStore, null, testDispatcher)
        advanceUntilIdle()

        // By default, 3 personas are selected = 12 widgets
        assertEquals(12, viewModel.activePersonaWidgets.value.size)
        assertEquals(1, forecastFetchCount)

        // Change persona selection to health_conscious only
        settingsStore.saveSelectedPersonas(setOf("health_conscious"))
        advanceUntilIdle()

        // Should update activePersonaWidgets to 4 health widgets without re-fetching forecast
        assertEquals(setOf("health_conscious"), viewModel.selectedPersonas.value)
        assertEquals(4, viewModel.activePersonaWidgets.value.size)
        assertTrue(viewModel.activePersonaWidgets.value.contains(com.weatherengine.app.persona.PersonaWidgetType.AQI))
        assertTrue(viewModel.activePersonaWidgets.value.contains(com.weatherengine.app.persona.PersonaWidgetType.POLLEN))
        assertEquals(1, forecastFetchCount) // No extra network call!
    }

    @Test
    fun testNoLoginNoBackend_ProducesForecastAndPersonaCards() = runTest(testDispatcher) {
        fakeForecastRepo.shouldSucceed = true
        val fakeEngineRepo = object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String) = error("unused")
            override suspend fun login(email: String, password: String) = error("unused")
            override suspend fun getPersonas() = error("unused")
            override suspend fun setPersonas(codes: List<String>) = error("unused")
            override suspend fun getHomepage(lat: Double, lon: Double) = error("unused")
        }

        // Unauthenticated, no backend running
        val viewModel = HomeViewModel(fakeForecastRepo, fakeEngineRepo, settingsStore, null, testDispatcher)
        advanceUntilIdle()

        // Forecast is loaded successfully
        assertTrue(viewModel.forecastState.value is ForecastUiState.Success)
        val forecast = (viewModel.forecastState.value as ForecastUiState.Success).forecast
        assertEquals("31°", forecast.current.temperatureFormatted)

        // Persona cards are computed on-device
        assertTrue(viewModel.activePersonaWidgets.value.isNotEmpty())
        assertEquals(12, viewModel.activePersonaWidgets.value.size)
    }
}
