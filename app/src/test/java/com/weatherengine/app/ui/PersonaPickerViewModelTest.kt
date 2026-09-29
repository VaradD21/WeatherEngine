package com.weatherengine.app.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.ui.personas.PersonaPickerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
class PersonaPickerViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsStore: SettingsStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val file = File(tempFolder.root, "test_persona_picker_vm.preferences_pb")
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
    fun testInitialStateLoadsFromSettingsStore() = runTest(testDispatcher) {
        val viewModel = PersonaPickerViewModel(settingsStore, testDispatcher)
        advanceUntilIdle()

        // Default set in SettingsStore is all 3 personas
        val loaded = viewModel.uiState.value.selectedCodes
        assertEquals(3, loaded.size)
        assertTrue(loaded.contains("health_conscious"))
        assertTrue(loaded.contains("outdoor_fitness"))
        assertTrue(loaded.contains("commuter"))
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testSelectingZeroPersonasShowsErrorAndRejectsSave() = runTest(testDispatcher) {
        val viewModel = PersonaPickerViewModel(settingsStore, testDispatcher)
        advanceUntilIdle()

        // Deselect all
        viewModel.toggleSelection("health_conscious")
        viewModel.toggleSelection("outdoor_fitness")
        viewModel.toggleSelection("commuter")

        assertEquals(0, viewModel.uiState.value.selectedCodes.size)

        // Try save
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Please select at least one persona", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)

        // Verify DataStore was not updated to empty
        val stored = settingsStore.selectedPersonasFlow.first()
        assertTrue(stored.isNotEmpty())
    }

    @Test
    fun testSavingPersistsAndSurvivesViewModelRecreation() = runTest(testDispatcher) {
        val viewModel1 = PersonaPickerViewModel(settingsStore, testDispatcher)
        advanceUntilIdle()

        // Toggle to select only "outdoor_fitness"
        viewModel1.toggleSelection("health_conscious")
        viewModel1.toggleSelection("commuter")

        assertEquals(setOf("outdoor_fitness"), viewModel1.uiState.value.selectedCodes)

        viewModel1.save()
        advanceUntilIdle()

        // Verify in settingsStore
        val saved = settingsStore.selectedPersonasFlow.first()
        assertEquals(setOf("outdoor_fitness"), saved)

        // Recreate ViewModel
        val viewModel2 = PersonaPickerViewModel(settingsStore, testDispatcher)
        advanceUntilIdle()

        assertEquals(setOf("outdoor_fitness"), viewModel2.uiState.value.selectedCodes)
    }

    @Test
    fun testToggleSelectionAddsAndRemoves() = runTest(testDispatcher) {
        val viewModel = PersonaPickerViewModel(settingsStore, testDispatcher)
        advanceUntilIdle()

        // Remove commuter
        viewModel.toggleSelection("commuter")
        assertFalse(viewModel.uiState.value.selectedCodes.contains("commuter"))

        // Add commuter back
        viewModel.toggleSelection("commuter")
        assertTrue(viewModel.uiState.value.selectedCodes.contains("commuter"))
    }
}
