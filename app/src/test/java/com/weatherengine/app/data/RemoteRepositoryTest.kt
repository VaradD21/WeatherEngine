package com.weatherengine.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.weatherengine.app.data.api.ApiService
import com.weatherengine.app.data.api.AuthInterceptor
import com.weatherengine.app.data.api.FailureKind
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.repository.RemoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var settingsStore: SettingsStore
    private lateinit var repository: RemoteRepository
    private var testToken: String? = null

    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val dataStoreFile = File(tempFolder.root, "test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher),
            produceFile = { dataStoreFile }
        )
        settingsStore = SettingsStore(dataStore)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .addInterceptor(AuthInterceptor { testToken })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        repository = RemoteRepository(
            apiServiceProvider = { apiService },
            settingsStore = settingsStore,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testLogin_200Success_NoAuthHeaderSent() = runTest(testDispatcher) {
        testToken = "some_existing_token"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"userId": 1, "email": "test@example.com", "token": "jwt_token_123"}""")
        )

        val result = repository.login("test@example.com", "password123")
        assertTrue(result is NetworkResult.Success)
        assertEquals("jwt_token_123", (result as NetworkResult.Success).data.token)

        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun testLogin_401BadCredentials() = runTest(testDispatcher) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": "Unauthorized", "message": "Bad credentials"}""")
        )

        val result = repository.login("test@example.com", "wrong")
        assertTrue(result is NetworkResult.Failure)
        assertEquals(FailureKind.Unauthorized, (result as NetworkResult.Failure).kind)
        assertEquals("Bad credentials", result.message)
    }

    @Test
    fun testSignup_409EmailExists() = runTest(testDispatcher) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": "Conflict", "message": "Email is already registered"}""")
        )

        val result = repository.signup("test@example.com", "password123")
        assertTrue(result is NetworkResult.Failure)
        assertEquals(FailureKind.Client, (result as NetworkResult.Failure).kind)
        assertEquals("Email is already registered", result.message)
    }

    @Test
    fun testValidation_400Shapes() = runTest(testDispatcher) {
        // Shape 1: { "message": "..." }
        mockWebServer.enqueue(
            MockResponse().setResponseCode(400).setBody("""{"message": "Invalid password"}""")
        )
        val res1 = repository.signup("a@b.com", "short")
        assertTrue(res1 is NetworkResult.Failure)
        assertEquals("Invalid password", (res1 as NetworkResult.Failure).message)

        // Shape 2: Map of field -> error
        mockWebServer.enqueue(
            MockResponse().setResponseCode(400).setBody("""{"email": "Must be valid"}""")
        )
        val res2 = repository.signup("invalid", "password123")
        assertTrue(res2 is NetworkResult.Failure)
        assertTrue((res2 as NetworkResult.Failure).message.contains("email: Must be valid"))

        // Shape 3: List of errors
        mockWebServer.enqueue(
            MockResponse().setResponseCode(400).setBody("""{"errors": [{"defaultMessage": "Email required"}]}""")
        )
        val res3 = repository.signup("", "password123")
        assertTrue(res3 is NetworkResult.Failure)
        assertTrue((res3 as NetworkResult.Failure).message.contains("Email required"))
    }

    @Test
    fun testHomepage_200Success_AuthHeaderSent() = runTest(testDispatcher) {
        testToken = "my_secret_token"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"widgets": [{"type": "aqi_card", "data": {"aqi": 1, "category": "Good"}}]}""")
        )

        val result = repository.getHomepage(19.0760, 72.8777)
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.widgets.size)

        val recorded = mockWebServer.takeRequest()
        assertEquals("Bearer my_secret_token", recorded.getHeader("Authorization"))
    }

    @Test
    fun testProtectedCall_401ClearsTokenAndEmitsSessionExpired() = runTest(testDispatcher) {
        testToken = "expired_token"
        settingsStore.saveAuth("expired_token", "user@example.com")
        mockWebServer.enqueue(
            MockResponse().setResponseCode(401).setBody("""{"message": "Token expired"}""")
        )

        val result = repository.getPersonas()
        assertTrue(result is NetworkResult.Failure)
        assertEquals(FailureKind.Unauthorized, (result as NetworkResult.Failure).kind)

        // Verify token cleared
        val storedToken = settingsStore.tokenFlow.first()
        assertNull(storedToken)
    }

    @Test
    fun test500WithHtml_NoCrash() = runTest(testDispatcher) {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(500).setBody("<html><body>Internal Error</body></html>")
        )

        val result = repository.getHomepage(1.0, 1.0)
        assertTrue(result is NetworkResult.Failure)
        assertEquals(FailureKind.Server, (result as NetworkResult.Failure).kind)
    }

    @Test
    fun testMalformedJsonAndEmptyBody() = runTest(testDispatcher) {
        // Empty body
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val emptyResult = repository.getHomepage(1.0, 1.0)
        assertTrue(emptyResult is NetworkResult.Failure)

        // Malformed JSON
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))
        val malformedResult = repository.getHomepage(1.0, 1.0)
        assertTrue(malformedResult is NetworkResult.Failure)
    }

    @Test
    fun testNetworkFailure_ServerShutdown() = runTest(testDispatcher) {
        mockWebServer.shutdown()
        val result = repository.getHomepage(1.0, 1.0)
        assertTrue(result is NetworkResult.Failure)
        assertEquals(FailureKind.Network, (result as NetworkResult.Failure).kind)
    }

    @Test
    fun testTimeout() = runTest(testDispatcher) {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
        )
        val result = repository.getHomepage(1.0, 1.0)
        assertTrue(result is NetworkResult.Failure)
        assertEquals(FailureKind.Timeout, (result as NetworkResult.Failure).kind)
    }
}
