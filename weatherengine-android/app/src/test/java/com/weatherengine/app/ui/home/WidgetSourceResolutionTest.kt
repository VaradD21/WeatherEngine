package com.weatherengine.app.ui.home

import com.weatherengine.app.core.WidgetUi
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSourceResolutionTest {

    @Test
    fun `guest user renders on-device persona widgets`() {
        val source = resolveWidgetSource(
            isAuthenticated = false,
            homeUiState = HomeUiState.Guest
        )
        assertEquals(WidgetRenderSource.LOCAL_PERSONA, source)
    }

    @Test
    fun `guest user in loading state renders on-device persona widgets`() {
        val source = resolveWidgetSource(
            isAuthenticated = false,
            homeUiState = HomeUiState.Loading
        )
        assertEquals(WidgetRenderSource.LOCAL_PERSONA, source)
    }

    @Test
    fun `authenticated user with Content renders backend widgets exclusively`() {
        val content = HomeUiState.Content(
            widgets = listOf(
                WidgetUi.StatusOnly("aqi_card", "ok", "AQI is good")
            )
        )
        val source = resolveWidgetSource(
            isAuthenticated = true,
            homeUiState = content
        )
        assertEquals(WidgetRenderSource.BACKEND, source)
    }

    @Test
    fun `authenticated user in loading state falls back to on-device persona widgets`() {
        val source = resolveWidgetSource(
            isAuthenticated = true,
            homeUiState = HomeUiState.Loading
        )
        assertEquals(WidgetRenderSource.LOCAL_PERSONA, source)
    }

    @Test
    fun `authenticated user in error state falls back to on-device persona widgets`() {
        val source = resolveWidgetSource(
            isAuthenticated = true,
            homeUiState = HomeUiState.Error("Backend unavailable")
        )
        assertEquals(WidgetRenderSource.LOCAL_PERSONA, source)
    }

    @Test
    fun `authenticated user in empty state falls back to on-device persona widgets`() {
        val source = resolveWidgetSource(
            isAuthenticated = true,
            homeUiState = HomeUiState.Empty
        )
        assertEquals(WidgetRenderSource.LOCAL_PERSONA, source)
    }
}
