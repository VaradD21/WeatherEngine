package com.weatherengine.app.ui.customize

import com.weatherengine.app.data.local.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomizeWidgetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsStore: SettingsStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsStore = SettingsStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultRegistry_containsExpectedCards() = runTest(testDispatcher) {
        val viewModel = CustomizeWidgetsViewModel(settingsStore)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.type == "school_commute_card" })
        assertTrue(items.any { it.type == "aqi_card" })
        assertTrue(items.any { it.type == "sunrise_sunset_card" })
        assertTrue(items.any { it.type == "best_running_hours_card" })
        assertTrue(items.any { it.type == "traffic_card" })
        assertTrue(items.any { it.type == "soil_moisture_card" })
        assertTrue(items.any { it.type == "extended_forecast_card" })
    }

    @Test
    fun testMoveUpAndDown_updatesOrder() = runTest(testDispatcher) {
        val viewModel = CustomizeWidgetsViewModel(settingsStore)
        advanceUntilIdle()

        val initialFirst = viewModel.uiState.value.items[0]
        val initialSecond = viewModel.uiState.value.items[1]

        viewModel.moveDown(0)
        advanceUntilIdle()

        assertEquals(initialSecond.type, viewModel.uiState.value.items[0].type)
        assertEquals(initialFirst.type, viewModel.uiState.value.items[1].type)

        viewModel.moveUp(1)
        advanceUntilIdle()

        assertEquals(initialFirst.type, viewModel.uiState.value.items[0].type)
        assertEquals(initialSecond.type, viewModel.uiState.value.items[1].type)
    }

    @Test
    fun testToggleVisibility_disablesWidget() = runTest(testDispatcher) {
        val viewModel = CustomizeWidgetsViewModel(settingsStore)
        advanceUntilIdle()

        val target = viewModel.uiState.value.items[0]
        assertTrue(target.isEnabled)

        viewModel.toggleVisibility(target.type, false)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.items.first { it.type == target.type }
        assertFalse(updated.isEnabled)
    }

    @Test
    fun testResetToDefault_resetsList() = runTest(testDispatcher) {
        val viewModel = CustomizeWidgetsViewModel(settingsStore)
        advanceUntilIdle()

        viewModel.moveDown(0)
        advanceUntilIdle()

        viewModel.resetToDefault()
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertEquals(CustomizeWidgetsViewModel.defaultWidgetRegistry().first().type, items.first().type)
    }
}
