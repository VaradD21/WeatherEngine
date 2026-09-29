package com.weatherengine.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.weatherengine.app.ui.components.MockBanner
import org.junit.Rule
import org.junit.Test

class ComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMockBanner_IsDisplayed() {
        composeTestRule.setContent {
            MockBanner()
        }

        composeTestRule.onNodeWithText("MOCK MODE — sample data").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Mock mode banner").assertIsDisplayed()
    }
}
