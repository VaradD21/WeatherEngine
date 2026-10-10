package com.weatherengine.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_engine_settings")

open class SettingsStore(private val dataStore: DataStore<Preferences>? = null) {

    companion object {
        const val CURRENT_FORECAST_SCHEMA_VERSION = 2

        val KEY_TOKEN = stringPreferencesKey("auth_token")
        val KEY_EMAIL = stringPreferencesKey("auth_email")
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_MANUAL_LAT = doublePreferencesKey("manual_lat")
        val KEY_MANUAL_LON = doublePreferencesKey("manual_lon")
        val KEY_CACHED_HOMEPAGE_JSON = stringPreferencesKey("cached_homepage_json")
        val KEY_CACHED_HOMEPAGE_TIME = longPreferencesKey("cached_homepage_time")

        const val EMULATOR_BASE_URL = "http://10.0.2.2:8080"
        const val USB_ADB_BASE_URL = "http://localhost:8080"
        val DEFAULT_PHONE_LAN_URL: String = try {
            com.weatherengine.app.BuildConfig.PHONE_LAN_URL
        } catch (_: Throwable) {
            "http://192.168.1.100:8080"
        }
        val DEFAULT_BASE_URL: String = try {
            com.weatherengine.app.BuildConfig.DEFAULT_BASE_URL
        } catch (_: Throwable) {
            EMULATOR_BASE_URL
        }
        const val DEFAULT_LAT = 19.0760
        const val DEFAULT_LON = 72.8777

        val KEY_SAVED_PHONE_URL = stringPreferencesKey("saved_phone_url")

        val KEY_CACHED_FORECAST_JSON = stringPreferencesKey("cached_forecast_json")
        val KEY_CACHED_FORECAST_TIME = longPreferencesKey("cached_forecast_time")
        val KEY_CACHED_FORECAST_LOC_KEY = stringPreferencesKey("cached_forecast_loc_key")
        val KEY_CACHED_FORECAST_SCHEMA = intPreferencesKey("cached_forecast_schema")

        val KEY_SELECTED_PERSONAS = stringSetPreferencesKey("selected_personas")
        val DEFAULT_PERSONAS = setOf("health_conscious", "outdoor_fitness", "commuter")

        val KEY_SCHOOL_START = stringPreferencesKey("school_start")
        val KEY_SCHOOL_END = stringPreferencesKey("school_end")
        const val DEFAULT_SCHOOL_START = "08:00"
        const val DEFAULT_SCHOOL_END = "15:00"
    }

    private val safePreferences: Flow<Preferences> = dataStore?.data
        ?.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        } ?: kotlinx.coroutines.flow.emptyFlow()

    open val tokenFlow: Flow<String?> = safePreferences.map { it[KEY_TOKEN] }
    open val baseUrlFlow: Flow<String> = safePreferences.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }
    open val manualLatFlow: Flow<Double> = safePreferences.map { it[KEY_MANUAL_LAT] ?: DEFAULT_LAT }
    open val manualLonFlow: Flow<Double> = safePreferences.map { it[KEY_MANUAL_LON] ?: DEFAULT_LON }
    open val cachedHomepageJsonFlow: Flow<String?> = safePreferences.map { it[KEY_CACHED_HOMEPAGE_JSON] }
    open val cachedHomepageTimeFlow: Flow<Long?> = safePreferences.map { it[KEY_CACHED_HOMEPAGE_TIME] }
    open val cachedForecastJsonFlow: Flow<String?> = safePreferences.map { it[KEY_CACHED_FORECAST_JSON] }
    open val cachedForecastLocKeyFlow: Flow<String?> = safePreferences.map { it[KEY_CACHED_FORECAST_LOC_KEY] }
    open val cachedForecastSchemaFlow: Flow<Int?> = safePreferences.map { it[KEY_CACHED_FORECAST_SCHEMA] }

    open val schoolStartFlow: Flow<String> = safePreferences.map { it[KEY_SCHOOL_START] ?: DEFAULT_SCHOOL_START }
    open val schoolEndFlow: Flow<String> = safePreferences.map { it[KEY_SCHOOL_END] ?: DEFAULT_SCHOOL_END }

    open val selectedPersonasFlow: Flow<Set<String>> = safePreferences.map {
        it[KEY_SELECTED_PERSONAS] ?: DEFAULT_PERSONAS
    }

    open suspend fun saveAuth(token: String, email: String) {
        dataStore?.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_EMAIL] = email
        }
    }

    open suspend fun clearToken() {
        dataStore?.edit { prefs ->
            prefs.remove(KEY_TOKEN)
        }
    }

    open suspend fun clearAll() {
        dataStore?.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_CACHED_HOMEPAGE_JSON)
            prefs.remove(KEY_CACHED_HOMEPAGE_TIME)
        }
    }

    open suspend fun setBaseUrl(url: String) {
        dataStore?.edit { prefs ->
            prefs[KEY_BASE_URL] = url
        }
    }

    open suspend fun setManualLocation(lat: Double, lon: Double) {
        dataStore?.edit { prefs ->
            prefs[KEY_MANUAL_LAT] = lat
            prefs[KEY_MANUAL_LON] = lon
        }
    }

    open suspend fun cacheHomepage(jsonString: String, timestampMs: Long) {
        dataStore?.edit { prefs ->
            prefs[KEY_CACHED_HOMEPAGE_JSON] = jsonString
            prefs[KEY_CACHED_HOMEPAGE_TIME] = timestampMs
        }
    }

    open suspend fun cacheForecast(
        locKey: String,
        jsonString: String,
        timestampMs: Long,
        schemaVersion: Int = CURRENT_FORECAST_SCHEMA_VERSION
    ) {
        dataStore?.edit { prefs ->
            prefs[KEY_CACHED_FORECAST_LOC_KEY] = locKey
            prefs[KEY_CACHED_FORECAST_JSON] = jsonString
            prefs[KEY_CACHED_FORECAST_TIME] = timestampMs
            prefs[KEY_CACHED_FORECAST_SCHEMA] = schemaVersion
        }
    }

    open suspend fun saveSelectedPersonas(codes: Set<String>) {
        dataStore?.edit { prefs ->
            prefs[KEY_SELECTED_PERSONAS] = codes
        }
    }

    open suspend fun saveSchoolHours(start: String, end: String) {
        dataStore?.edit { prefs ->
            prefs[KEY_SCHOOL_START] = start
            prefs[KEY_SCHOOL_END] = end
        }
    }

    open val savedPhoneUrlFlow: Flow<String> = safePreferences.map {
        it[KEY_SAVED_PHONE_URL] ?: DEFAULT_PHONE_LAN_URL
    }

    open suspend fun savePhoneUrl(url: String) {
        dataStore?.edit { prefs ->
            prefs[KEY_SAVED_PHONE_URL] = url
        }
    }
}
