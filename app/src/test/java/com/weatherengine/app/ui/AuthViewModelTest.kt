package com.weatherengine.app.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.weatherengine.app.data.api.FailureKind
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.model.AuthResponse
import com.weatherengine.app.data.repository.WeatherEngineRepository
import com.weatherengine.app.ui.auth.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsStore: SettingsStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val file = File(tempFolder.root, "test_auth_vm.preferences_pb")
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
    fun testLogin_Success() = runTest(testDispatcher) {
        val fakeRepo = object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String) = error("unused")
            override suspend fun login(email: String, password: String): NetworkResult<AuthResponse> {
                return NetworkResult.Success(AuthResponse(userId = 1L, email = email, token = "test_token"))
            }
            override suspend fun getPersonas() = error("unused")
            override suspend fun setPersonas(codes: List<String>) = error("unused")
            override suspend fun getHomepage(lat: Double, lon: Double) = error("unused")
        }

        val viewModel = AuthViewModel(fakeRepo, settingsStore, testDispatcher)
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")

        viewModel.submit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        val savedToken = settingsStore.tokenFlow.first()
        assertEquals("test_token", savedToken)
    }

    @Test
    fun testLogin_Failure() = runTest(testDispatcher) {
        val fakeRepo = object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String) = error("unused")
            override suspend fun login(email: String, password: String): NetworkResult<AuthResponse> {
                return NetworkResult.Failure(FailureKind.Unauthorized, "Invalid credentials")
            }
            override suspend fun getPersonas() = error("unused")
            override suspend fun setPersonas(codes: List<String>) = error("unused")
            override suspend fun getHomepage(lat: Double, lon: Double) = error("unused")
        }

        val viewModel = AuthViewModel(fakeRepo, settingsStore, testDispatcher)
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("wrongpassword")

        viewModel.submit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Invalid credentials", viewModel.uiState.value.generalError)
    }

    @Test
    fun testDoubleSubmitIgnoredWhileInFlight() = runTest(testDispatcher) {
        var callCount = 0
        val fakeRepo = object : WeatherEngineRepository {
            override suspend fun signup(email: String, password: String) = error("unused")
            override suspend fun login(email: String, password: String): NetworkResult<AuthResponse> {
                callCount++
                return NetworkResult.Success(AuthResponse(1L, email, "tok"))
            }
            override suspend fun getPersonas() = error("unused")
            override suspend fun setPersonas(codes: List<String>) = error("unused")
            override suspend fun getHomepage(lat: Double, lon: Double) = error("unused")
        }

        val viewModel = AuthViewModel(fakeRepo, settingsStore, testDispatcher)
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")

        viewModel.submit()
        assertTrue(viewModel.uiState.value.isLoading)

        // Second call while first is in flight
        viewModel.submit()

        advanceUntilIdle()
        assertEquals(1, callCount)
    }
}
