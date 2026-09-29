package com.weatherengine.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.openmeteo.OpenMeteoClient
import com.weatherengine.app.data.repository.OpenMeteoForecastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OpenMeteoForecastRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockWebServer: MockWebServer
    private lateinit var settingsStore: SettingsStore
    private lateinit var repository: OpenMeteoForecastRepository

    private val forecastJson: String by lazy {
        val stream = javaClass.classLoader?.getResourceAsStream("openmeteo-forecast.json")
        stream!!.bufferedReader().readText()
    }

    private val aqiJson: String by lazy {
        val stream = javaClass.classLoader?.getResourceAsStream("openmeteo-air-quality.json")
        stream!!.bufferedReader().readText()
    }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val file = File(tempFolder.root, "test_forecast.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(testDispatcher),
            produceFile = { file }
        )
        settingsStore = SettingsStore(dataStore)

        val baseUrl = mockWebServer.url("/").toString()
        val openMeteoClient = OpenMeteoClient(baseUrl = baseUrl)

        repository = OpenMeteoForecastRepository(
            apiService = openMeteoClient.apiService,
            settingsStore = settingsStore,
            airQualityUrl = mockWebServer.url("/v1/air-quality").toString()
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetForecast_Success_AndHeaderVerification() = runTest(testDispatcher) {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.contains("forecast") == true -> MockResponse().setResponseCode(200).setBody(forecastJson)
                    request.path?.contains("air-quality") == true -> MockResponse().setResponseCode(200).setBody(aqiJson)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val result = repository.getForecast(19.076, 72.877, "Mumbai", forceRefresh = true)

        assertTrue(result is NetworkResult.Success)
        val forecast = (result as NetworkResult.Success).data
        assertEquals("Mumbai", forecast.locationLabel)
        assertEquals("31°", forecast.current.temperatureFormatted)
        assertEquals(7, forecast.days.size)

        // Verify request headers
        val req = mockWebServer.takeRequest()
        assertEquals(OpenMeteoClient.USER_AGENT, req.getHeader("User-Agent"))
        assertNull("Open-Meteo requests must NEVER contain Authorization token", req.getHeader("Authorization"))
    }

    @Test
    fun testGetForecast_15MinCacheHit() = runTest(testDispatcher) {
        var networkCalls = 0
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                networkCalls++
                return when {
                    request.path?.contains("forecast") == true -> MockResponse().setResponseCode(200).setBody(forecastJson)
                    request.path?.contains("air-quality") == true -> MockResponse().setResponseCode(200).setBody(aqiJson)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val result1 = repository.getForecast(19.076, 72.877, "Mumbai", forceRefresh = false)
        assertTrue(result1 is NetworkResult.Success)
        assertFalse((result1 as NetworkResult.Success).data.isCached)
        val callsAfterFirst = networkCalls

        // Second call should hit the cache without hitting mockWebServer again
        val result2 = repository.getForecast(19.076, 72.877, "Mumbai", forceRefresh = false)
        assertTrue(result2 is NetworkResult.Success)
        assertTrue((result2 as NetworkResult.Success).data.isCached)
        assertEquals(callsAfterFirst, networkCalls)
    }

    @Test
    fun testGetForecast_NetworkFailure_FallsBackToCache() = runTest(testDispatcher) {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.contains("forecast") == true -> MockResponse().setResponseCode(200).setBody(forecastJson)
                    request.path?.contains("air-quality") == true -> MockResponse().setResponseCode(200).setBody(aqiJson)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        repository.getForecast(19.076, 72.877, "Mumbai", forceRefresh = true)

        // Now set dispatcher to 500 error
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setResponseCode(500).setBody("Server Error")
            }
        }

        val fallbackResult = repository.getForecast(19.076, 72.877, "Mumbai", forceRefresh = true)
        assertTrue(fallbackResult is NetworkResult.Success)
        val data = (fallbackResult as NetworkResult.Success).data
        assertTrue(data.isCached)
        assertTrue(data.updatedAtLabel.contains("offline"))
    }

    @Test
    fun testGetForecast_NetworkFailure_WithoutCache_ReturnsFailure() = runTest(testDispatcher) {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setResponseCode(500).setBody("Server Error")
            }
        }

        val result = repository.getForecast(10.0, 20.0, "Somewhere", forceRefresh = true)
        assertTrue(result is NetworkResult.Failure)
    }

    @Test
    fun testGetForecast_Schema1Cache_TreatedAsCacheMiss() = runTest(testDispatcher) {
        var networkCalls = 0
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                networkCalls++
                return when {
                    request.path?.contains("forecast") == true -> MockResponse().setResponseCode(200).setBody(forecastJson)
                    request.path?.contains("air-quality") == true -> MockResponse().setResponseCode(200).setBody(aqiJson)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        // Pre-populate cache with Schema 1 (Prompt A2 schema)
        settingsStore.cacheForecast(
            locKey = "19.08,72.88",
            jsonString = "{\"locationLabel\":\"Old\"}",
            timestampMs = System.currentTimeMillis(),
            schemaVersion = 1
        )

        // Since schema is 1 and current schema is 2, this must be a cache miss and fetch from network
        val result = repository.getForecast(19.076, 72.877, "Mumbai", forceRefresh = false)
        assertTrue(result is NetworkResult.Success)
        assertFalse((result as NetworkResult.Success).data.isCached)
        assertTrue(networkCalls > 0)
    }

    @Test
    fun testGetForecast_AirQualityFailure_ForecastStillSucceeds() = runTest(testDispatcher) {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.contains("forecast") == true -> MockResponse().setResponseCode(200).setBody(forecastJson)
                    request.path?.contains("air-quality") == true -> MockResponse().setResponseCode(500).setBody("AQI Error")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val result = repository.getForecast(19.076, 72.877, "Mumbai", forceRefresh = true)
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("31°", data.current.temperatureFormatted)
        assertNull(data.current.aqi)
        assertEquals("Unavailable", data.current.aqiCategory)
        assertFalse(data.pollen.isAvailable)
    }
}
