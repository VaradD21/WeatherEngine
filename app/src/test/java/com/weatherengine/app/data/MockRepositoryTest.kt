package com.weatherengine.app.data

import com.weatherengine.app.data.api.FailureKind
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.repository.MockRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = MockRepository(testDispatcher)

    @Test
    fun testAuth_SignupAndLogin() = runTest(testDispatcher) {
        // Signup
        val signupResult = repository.signup("newuser@example.com", "securePass123")
        assertTrue(signupResult is NetworkResult.Success)
        val token = (signupResult as NetworkResult.Success).data.token
        assertTrue(token.startsWith("mock."))

        // Login with correct credentials
        val loginResult = repository.login("newuser@example.com", "securePass123")
        assertTrue(loginResult is NetworkResult.Success)

        // Login with unknown email returns generic 401
        val unknownResult = repository.login("unknown@example.com", "securePass123")
        assertTrue(unknownResult is NetworkResult.Failure)
        assertEquals(FailureKind.Unauthorized, (unknownResult as NetworkResult.Failure).kind)
        assertEquals("Invalid email or password", unknownResult.message)

        // Login with wrong password returns the same generic 401
        val wrongPassResult = repository.login("newuser@example.com", "wrongPassword")
        assertTrue(wrongPassResult is NetworkResult.Failure)
        assertEquals(FailureKind.Unauthorized, (wrongPassResult as NetworkResult.Failure).kind)
        assertEquals("Invalid email or password", wrongPassResult.message)
    }

    @Test
    fun testPersonas_InvalidCodeReturnsClientFailure() = runTest(testDispatcher) {
        val result = repository.setPersonas(listOf("invalid_code_123"))
        assertTrue(result is NetworkResult.Failure)
        assertEquals(FailureKind.Client, (result as NetworkResult.Failure).kind)
    }

    @Test
    fun testHomepage_DeduplicatesWidgetsAndContainsMockedAndUnavailable() = runTest(testDispatcher) {
        // health_conscious and outdoor_fitness share aqi_card and uv_index_card; commuter has traffic_card (mocked)
        val setRes = repository.setPersonas(listOf("health_conscious", "outdoor_fitness", "commuter"))
        assertTrue(setRes is NetworkResult.Success)

        val homepageResult = repository.getHomepage(19.0760, 72.8777)
        assertTrue(homepageResult is NetworkResult.Success)
        val widgets = (homepageResult as NetworkResult.Success).data.widgets

        // Check deduplication
        val types = widgets.map { it.type }
        assertEquals(types.size, types.distinct().size)

        // Check presence of "mocked" or "unavailable" status in widgets
        var hasMocked = false
        var hasUnavailable = false
        for (w in widgets) {
            val status = (w.data as? JsonObject)?.get("status")?.jsonPrimitive?.content
            if (status == "mocked") hasMocked = true
            if (status == "unavailable") hasUnavailable = true
        }
        assertTrue(hasUnavailable)
        assertTrue(hasMocked)
    }
}
