package com.weatherengine.app.ui.home

import com.weatherengine.app.core.WidgetUi
import com.weatherengine.app.core.forecast.CurrentWeatherUi
import com.weatherengine.app.core.forecast.ForecastUi
import com.weatherengine.app.data.api.FailureKind
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.model.AuthResponse
import com.weatherengine.app.data.model.HomepageResponse
import com.weatherengine.app.data.model.PersonaDto
import com.weatherengine.app.data.model.WidgetDto
import com.weatherengine.app.data.repository.ForecastRepository
import com.weatherengine.app.data.repository.WeatherEngineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelParentTest {

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var fakeForecastRepo: FakeForecastRepository
    private lateinit var fakeWeatherRepo: FakeWeatherEngineRepository
    private lateinit var fakeSettingsStore: FakeSettingsStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeForecastRepo = FakeForecastRepository()
        fakeWeatherRepo = FakeWeatherEngineRepository()
        fakeSettingsStore = FakeSettingsStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testParentSelected_loadsThreeCardsOnline() = runTest(testDispatcher) {
        fakeSettingsStore.tokenState.value = "mock-token"
        fakeSettingsStore.personasState.value = setOf("parent")

        val commuteJson = json.parseToJsonElement("""{
            "status": "ok",
            "run": "morning",
            "date": "2026-10-12",
            "window": "07:00 - 08:00",
            "verdict": "good",
            "reasons": ["Comfortable weather"],
            "tips": []
        }""")
        val rainJson = json.parseToJsonElement("""{
            "status": "ok",
            "message": "No rain expected in the next 6 hours",
            "peakProbabilityPercent": 5
        }""")
        val severeJson = json.parseToJsonElement("""{
            "status": "ok",
            "alerts": [],
            "message": "No severe weather expected in the next 24 hours"
        }""")

        fakeWeatherRepo.homepageResponseToReturn = HomepageResponse(
            widgets = listOf(
                WidgetDto("school_commute_card", commuteJson),
                WidgetDto("rain_alert_card", rainJson),
                WidgetDto("severe_weather_card", severeJson)
            )
        )

        val viewModel = HomeViewModel(
            forecastRepository = fakeForecastRepo,
            repository = fakeWeatherRepo,
            settingsStore = fakeSettingsStore,
            ioDispatcher = testDispatcher
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected HomeUiState.Content but was $state", state is HomeUiState.Content)
        val content = state as HomeUiState.Content
        assertEquals(3, content.widgets.size)
        assertTrue(content.widgets[0] is WidgetUi.SchoolCommute)
        assertTrue(content.widgets[1] is WidgetUi.RainAlert)
        assertTrue(content.widgets[2] is WidgetUi.SevereWeather)
    }

    @Test
    fun testParentSelected_offlineShowsAvailableWhenOnline() = runTest(testDispatcher) {
        fakeSettingsStore.tokenState.value = "mock-token"
        fakeSettingsStore.personasState.value = setOf("parent")
        fakeWeatherRepo.shouldFail = true // simulate network failure

        val viewModel = HomeViewModel(
            forecastRepository = fakeForecastRepo,
            repository = fakeWeatherRepo,
            settingsStore = fakeSettingsStore,
            ioDispatcher = testDispatcher
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected HomeUiState.Content but was $state", state is HomeUiState.Content)
        val content = state as HomeUiState.Content
        assertEquals(3, content.widgets.size)

        for (widget in content.widgets) {
            assertTrue(widget is WidgetUi.StatusOnly)
            val statusOnly = widget as WidgetUi.StatusOnly
            assertEquals("unavailable", statusOnly.status)
            assertEquals("Available when online", statusOnly.message)
        }
    }

    @Test
    fun testParentSelected_loggedOutGuestShowsAvailableWhenOnline() = runTest(testDispatcher) {
        fakeSettingsStore.tokenState.value = null // logged out guest
        fakeSettingsStore.personasState.value = setOf("parent")

        val viewModel = HomeViewModel(
            forecastRepository = fakeForecastRepo,
            repository = fakeWeatherRepo,
            settingsStore = fakeSettingsStore,
            ioDispatcher = testDispatcher
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected HomeUiState.Content but was $state", state is HomeUiState.Content)
        val content = state as HomeUiState.Content
        assertEquals(3, content.widgets.size)

        for (widget in content.widgets) {
            assertTrue(widget is WidgetUi.StatusOnly)
            val statusOnly = widget as WidgetUi.StatusOnly
            assertEquals("unavailable", statusOnly.status)
            assertEquals("Available when online", statusOnly.message)
        }
    }

    @Test
    fun testSchoolHoursChange_triggersReloadAndSendsQueryParams() = runTest(testDispatcher) {
        fakeSettingsStore.tokenState.value = "mock-token"
        fakeSettingsStore.personasState.value = setOf("parent")
        fakeSettingsStore.schoolStartState.value = "08:00"
        fakeSettingsStore.schoolEndState.value = "15:00"

        val viewModel = HomeViewModel(
            forecastRepository = fakeForecastRepo,
            repository = fakeWeatherRepo,
            settingsStore = fakeSettingsStore,
            ioDispatcher = testDispatcher
        )

        advanceUntilIdle()

        assertEquals("08:00", fakeWeatherRepo.lastSchoolStart)
        assertEquals("15:00", fakeWeatherRepo.lastSchoolEnd)
        val initialCallCount = fakeWeatherRepo.callCount
        assertTrue(initialCallCount >= 1)

        // Change school hours in SettingsStore
        fakeSettingsStore.saveSchoolHours("07:30", "14:30")
        advanceUntilIdle()

        assertEquals("07:30", fakeWeatherRepo.lastSchoolStart)
        assertEquals("14:30", fakeWeatherRepo.lastSchoolEnd)
        assertTrue(fakeWeatherRepo.callCount > initialCallCount)
        assertTrue(viewModel.locationLabel.value.isNotEmpty())
    }

    private class FakeForecastRepository : ForecastRepository {
        override suspend fun getForecast(
            lat: Double,
            lon: Double,
            locationLabel: String,
            forceRefresh: Boolean
        ): NetworkResult<ForecastUi> {
            return NetworkResult.Success(
                ForecastUi(
                    locationLabel = locationLabel,
                    updatedAtLabel = "Just now",
                    timestampMs = System.currentTimeMillis(),
                    current = CurrentWeatherUi(
                        temperatureFormatted = "25°",
                        apparentTemperatureFormatted = "Feels like 25°",
                        condition = "Sunny",
                        iconKey = "01d",
                        humidityFormatted = "50%",
                        windFormatted = "10 km/h",
                        precipitationFormatted = "0.0 mm"
                    )
                )
            )
        }
    }

    private class FakeWeatherEngineRepository : WeatherEngineRepository {
        var lastSchoolStart: String? = null
        var lastSchoolEnd: String? = null
        var callCount: Int = 0
        var shouldFail: Boolean = false
        var homepageResponseToReturn: HomepageResponse = HomepageResponse(emptyList())

        override suspend fun signup(email: String, password: String): NetworkResult<AuthResponse> = error("Not needed")
        override suspend fun login(email: String, password: String): NetworkResult<AuthResponse> = error("Not needed")
        override suspend fun getPersonas(): NetworkResult<List<PersonaDto>> = error("Not needed")
        override suspend fun setPersonas(codes: List<String>): NetworkResult<List<PersonaDto>> = error("Not needed")

        override suspend fun getHomepage(
            lat: Double,
            lon: Double,
            schoolStart: String?,
            schoolEnd: String?
        ): NetworkResult<HomepageResponse> {
            callCount++
            lastSchoolStart = schoolStart
            lastSchoolEnd = schoolEnd
            return if (shouldFail) {
                NetworkResult.Failure(FailureKind.Network, "Network error")
            } else {
                NetworkResult.Success(homepageResponseToReturn)
            }
        }
    }

    private class FakeSettingsStore : SettingsStore() {
        val tokenState = MutableStateFlow<String?>("valid-token")
        val personasState = MutableStateFlow<Set<String>>(setOf("parent"))
        val schoolStartState = MutableStateFlow("08:00")
        val schoolEndState = MutableStateFlow("15:00")
        val latState = MutableStateFlow(19.0760)
        val lonState = MutableStateFlow(72.8777)
        val cachedJsonState = MutableStateFlow<String?>(null)
        val cachedTimeState = MutableStateFlow<Long?>(null)

        override val tokenFlow: Flow<String?> get() = tokenState
        override val selectedPersonasFlow: Flow<Set<String>> get() = personasState
        override val schoolStartFlow: Flow<String> get() = schoolStartState
        override val schoolEndFlow: Flow<String> get() = schoolEndState
        override val manualLatFlow: Flow<Double> get() = latState
        override val manualLonFlow: Flow<Double> get() = lonState
        override val cachedHomepageJsonFlow: Flow<String?> get() = cachedJsonState
        override val cachedHomepageTimeFlow: Flow<Long?> get() = cachedTimeState

        override suspend fun saveSchoolHours(start: String, end: String) {
            schoolStartState.value = start
            schoolEndState.value = end
        }
    }
}
