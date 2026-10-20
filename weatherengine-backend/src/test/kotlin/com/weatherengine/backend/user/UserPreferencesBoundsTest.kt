package com.weatherengine.backend.user

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import java.util.Optional
import java.util.UUID

/** Tests for REL-05: widgetOrder/disabledWidgets size limits, unknown codes, bad enum values. */
class UserPreferencesBoundsTest {

    private val repo = mockk<UserPreferencesRepository>()
    private val controller = UserPreferencesController(repo)
    private val userId = UUID.randomUUID()
    private val principal = AuthenticatedUser(userId, "u@e.com")

    private fun stubRepo() {
        every { repo.findById(userId) } returns Optional.of(UserPreferences(userId = userId))
        every { repo.save(any()) } answers { firstArg() }
    }

    @Test
    fun `widgetOrder with more than 30 items is rejected with 400`() {
        stubRepo()
        val dto = UserPreferencesDto(
            widgetOrder = (1..31).map { "widget_$it" }
        )
        val ex = assertThrows<ApiException> {
            controller.updatePreferences(principal, dto)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
    }

    @Test
    fun `disabledWidgets with more than 30 items is rejected with 400`() {
        stubRepo()
        val dto = UserPreferencesDto(
            disabledWidgets = (1..31).map { "widget_$it" }.toSet()
        )
        val ex = assertThrows<ApiException> {
            controller.updatePreferences(principal, dto)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
    }

    @ParameterizedTest(name = "unknown widget code ''{0}'' rejected")
    @ValueSource(strings = ["hack_card", "../../etc/passwd", "unknown_widget_xyz", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"])
    fun `unknown or oversized widget codes in widgetOrder are rejected with 400`(code: String) {
        stubRepo()
        val dto = UserPreferencesDto(widgetOrder = listOf(code))
        val ex = assertThrows<ApiException> {
            controller.updatePreferences(principal, dto)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
    }

    @ParameterizedTest(name = "unknown widget code ''{0}'' in disabledWidgets rejected")
    @ValueSource(strings = ["hack_card", "unknown_widget_xyz"])
    fun `unknown widget codes in disabledWidgets are rejected with 400`(code: String) {
        stubRepo()
        val dto = UserPreferencesDto(disabledWidgets = setOf(code))
        val ex = assertThrows<ApiException> {
            controller.updatePreferences(principal, dto)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
    }

    @ParameterizedTest(name = "bad temperatureUnit ''{0}'' rejected")
    @ValueSource(strings = ["KELVIN", "celsius", "F", "123", ""])
    fun `invalid temperatureUnit is rejected with 400`(unit: String) {
        stubRepo()
        val dto = UserPreferencesDto(temperatureUnit = unit)
        val ex = assertThrows<ApiException> {
            controller.updatePreferences(principal, dto)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
    }

    @ParameterizedTest(name = "bad defaultScreen ''{0}'' rejected")
    @ValueSource(strings = ["admin", "../../etc", "unknown_screen", ""])
    fun `invalid defaultScreen is rejected with 400`(screen: String) {
        stubRepo()
        val dto = UserPreferencesDto(defaultScreen = screen)
        val ex = assertThrows<ApiException> {
            controller.updatePreferences(principal, dto)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
    }

    @Test
    fun `valid FAHRENHEIT temperatureUnit is accepted`() {
        stubRepo()
        val dto = UserPreferencesDto(temperatureUnit = "FAHRENHEIT")
        val response = controller.updatePreferences(principal, dto)
        assertEquals("FAHRENHEIT", response.body?.temperatureUnit)
    }

    @Test
    fun `valid widget codes are accepted`() {
        stubRepo()
        val dto = UserPreferencesDto(
            widgetOrder = listOf("aqi_card", "traffic_card"),
            disabledWidgets = setOf("uv_index_card"),
            defaultScreen = "commute",
            temperatureUnit = "CELSIUS"
        )
        val response = controller.updatePreferences(principal, dto)
        assertEquals(listOf("aqi_card", "traffic_card"), response.body?.widgetOrder)
    }
}
