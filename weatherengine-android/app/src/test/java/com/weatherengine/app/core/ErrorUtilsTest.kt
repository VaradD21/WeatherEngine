package com.weatherengine.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorUtilsTest {

    @Test
    fun testDescribeApiError_emptyOrNull() {
        assertEquals("Invalid request", ErrorUtils.describeApiError(400, null))
        assertEquals("Invalid request", ErrorUtils.describeApiError(400, ""))
        assertEquals("Invalid request", ErrorUtils.describeApiError(400, "   "))
        assertEquals("Unauthorized", ErrorUtils.describeApiError(401, null))
        assertEquals("Email already registered", ErrorUtils.describeApiError(409, null))
        assertEquals("Server error", ErrorUtils.describeApiError(500, null))
    }

    @Test
    fun testDescribeApiError_plainStringAndHtml() {
        assertEquals("Something broke", ErrorUtils.describeApiError(400, "Something broke"))
        // HTML is replaced by default status message to avoid dumping markup
        assertEquals("Server error", ErrorUtils.describeApiError(500, "<html><body>500 Internal Server Error</body></html>"))
    }

    @Test
    fun testDescribeApiError_messageOnly() {
        val json = """{"message": "User not found"}"""
        assertEquals("User not found", ErrorUtils.describeApiError(404, json))
    }

    @Test
    fun testDescribeApiError_errorAndMessage() {
        val json = """{"error": "Bad Request", "message": "Invalid password length"}"""
        assertEquals("Invalid password length", ErrorUtils.describeApiError(400, json))
    }

    @Test
    fun testDescribeApiError_errorOnly() {
        val json = """{"error": "Unauthorized Access"}"""
        assertEquals("Unauthorized Access", ErrorUtils.describeApiError(401, json))
    }

    @Test
    fun testDescribeApiError_fieldErrorList() {
        val json = """
            {
                "errors": [
                    {"defaultMessage": "Email must not be blank"},
                    {"defaultMessage": "Password too short"}
                ]
            }
        """.trimIndent()
        val result = ErrorUtils.describeApiError(400, json)
        assertTrue(result.contains("Email must not be blank"))
        assertTrue(result.contains("Password too short"))
    }

    @Test
    fun testDescribeApiError_fieldErrorMap() {
        val json = """{"email": "Invalid format", "password": "Must be 8+ characters"}"""
        val result = ErrorUtils.describeApiError(400, json)
        assertTrue(result.contains("email: Invalid format"))
        assertTrue(result.contains("password: Must be 8+ characters"))
    }
}
