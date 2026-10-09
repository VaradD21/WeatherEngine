package com.weatherengine.app.ui.settings

import com.weatherengine.app.core.Validators
import com.weatherengine.app.data.local.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsValidationTest {

    @Test
    fun testFreshInstallDefaults() {
        assertEquals("08:00", SettingsStore.DEFAULT_SCHOOL_START)
        assertEquals("15:00", SettingsStore.DEFAULT_SCHOOL_END)

        // Fresh install defaults must be valid
        val error = Validators.validateSchoolHours(
            SettingsStore.DEFAULT_SCHOOL_START,
            SettingsStore.DEFAULT_SCHOOL_END
        )
        assertNull(error)
    }

    @Test
    fun testEndNotAfterStartRejected() {
        // Equal times
        val equalError = Validators.validateSchoolHours("08:00", "08:00")
        assertNotNull(equalError)
        assertEquals("School end time must be strictly after school start time", equalError)

        // End before start
        val beforeError = Validators.validateSchoolHours("15:00", "08:00")
        assertNotNull(beforeError)
        assertEquals("School end time must be strictly after school start time", beforeError)

        // End just 1 minute earlier
        val oneMinEarlier = Validators.validateSchoolHours("08:01", "08:00")
        assertNotNull(oneMinEarlier)
        assertEquals("School end time must be strictly after school start time", oneMinEarlier)
    }

    @Test
    fun testInvalidSchoolHourFormatsRejected() {
        val testCases = listOf(
            Pair("8:00", "15:00"),      // missing leading zero
            Pair("08:00", "3:00"),      // missing leading zero
            Pair("24:00", "25:00"),     // invalid hours
            Pair("08:60", "15:00"),     // invalid minutes
            Pair("08:00", "15:70"),     // invalid minutes
            Pair("invalid", "15:00"),   // non-time string
            Pair("08:00", "invalid"),   // non-time string
            Pair("", "15:00"),          // blank start
            Pair("08:00", ""),          // blank end
            Pair(null, "15:00"),        // null start
            Pair("08:00", null)         // null end
        )

        for ((start, end) in testCases) {
            val error = Validators.validateSchoolHours(start, end)
            assertNotNull("Expected error for start=$start end=$end", error)
            assertEquals("Both times must be in HH:mm 24-hour format (e.g. 08:00, 15:00)", error)
        }
    }

    @Test
    fun testValidSchoolHoursAccepted() {
        val validPairs = listOf(
            Pair("07:30", "14:30"),
            Pair("08:00", "15:00"),
            Pair("08:59", "09:00"),
            Pair("00:00", "23:59")
        )

        for ((start, end) in validPairs) {
            val error = Validators.validateSchoolHours(start, end)
            assertNull("Expected no error for start=$start end=$end", error)
        }
    }
}
