package com.weatherengine.app.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.KeyStoreException

class FakeSecureTokenStorage : SecureTokenStorage {
    var storedToken: String? = null
    var shouldThrowOnGet = false
    var shouldThrowOnSave = false

    override fun getToken(): String? {
        if (shouldThrowOnGet) throw KeyStoreException("Corrupted KeyStore key")
        return storedToken
    }

    override fun saveToken(token: String) {
        if (shouldThrowOnSave) throw KeyStoreException("Failed to encrypt token")
        storedToken = token
    }

    override fun clearToken() {
        storedToken = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SecureTokenStorageTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun createTestDataStore(): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        val file = File(tmp.root, "test_prefs_${System.currentTimeMillis()}.preferences_pb")
        return PreferenceDataStoreFactory.create { file }
    }

    @Test
    fun `saveAuth stores token in secure storage and not in DataStore`() = runTest {
        val dataStore = createTestDataStore()
        val fakeSecure = FakeSecureTokenStorage()
        val settingsStore = SettingsStore(dataStore, fakeSecure)

        settingsStore.saveAuth("secret-jwt-token-123", "user@example.com")

        assertEquals("secret-jwt-token-123", fakeSecure.storedToken)
        assertEquals("secret-jwt-token-123", settingsStore.tokenFlow.first())

        // Confirm token is NOT in plain DataStore
        val prefs = dataStore.data.first()
        assertNull(prefs[SettingsStore.KEY_TOKEN])
        assertEquals("user@example.com", prefs[SettingsStore.KEY_EMAIL])
    }

    @Test
    fun `migration transfers legacy plaintext token to secure storage and deletes plaintext`() = runTest {
        val dataStore = createTestDataStore()

        // Simulate legacy state: plaintext token exists in DataStore
        dataStore.edit { prefs ->
            prefs[SettingsStore.KEY_TOKEN] = "legacy-plaintext-token"
            prefs[SettingsStore.KEY_EMAIL] = "legacy@example.com"
        }

        val fakeSecure = FakeSecureTokenStorage()
        val settingsStore = SettingsStore(dataStore, fakeSecure)

        // Perform migration
        settingsStore.migrateTokenIfNeeded()

        // Token must now be in secure storage
        assertEquals("legacy-plaintext-token", fakeSecure.storedToken)
        assertEquals("legacy-plaintext-token", settingsStore.tokenFlow.first())

        // Plaintext token must be deleted from DataStore
        val prefs = dataStore.data.first()
        assertNull(prefs[SettingsStore.KEY_TOKEN])
        assertEquals("legacy@example.com", prefs[SettingsStore.KEY_EMAIL])
    }

    @Test
    fun `corruption or KeyStoreException in secure storage clears token without crashing`() = runTest {
        val dataStore = createTestDataStore()
        val fakeSecure = FakeSecureTokenStorage().apply {
            storedToken = "will-fail-to-decrypt"
            shouldThrowOnGet = true
        }

        val settingsStore = SettingsStore(dataStore, fakeSecure)
        settingsStore.migrateTokenIfNeeded()

        // Must safely emit null, forcing re-login instead of crashing
        assertNull(settingsStore.tokenFlow.first())
        assertNull(fakeSecure.storedToken)
    }

    @Test
    fun `clearToken removes token from secure storage and DataStore`() = runTest {
        val dataStore = createTestDataStore()
        val fakeSecure = FakeSecureTokenStorage().apply {
            storedToken = "token-to-delete"
        }
        val settingsStore = SettingsStore(dataStore, fakeSecure)

        settingsStore.clearToken()

        assertNull(fakeSecure.storedToken)
        assertNull(settingsStore.tokenFlow.first())
    }
}
