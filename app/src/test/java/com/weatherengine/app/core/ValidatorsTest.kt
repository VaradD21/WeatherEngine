package com.weatherengine.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun testIsValidEmail() {
        assertTrue(Validators.isValidEmail("user@example.com"))
        assertTrue(Validators.isValidEmail("test.user+tag@domain.co.uk"))

        assertFalse(Validators.isValidEmail("missing-at.com"))
        assertFalse(Validators.isValidEmail("user@missing-tld"))
        assertFalse(Validators.isValidEmail(" spaces @domain.com "))
        assertFalse(Validators.isValidEmail(""))
        assertFalse(Validators.isValidEmail("   "))
        assertFalse(Validators.isValidEmail(null))
    }

    @Test
    fun testIsValidPassword() {
        assertFalse(Validators.isValidPassword("1234567")) // 7 chars fails
        assertTrue(Validators.isValidPassword("12345678")) // 8 chars passes
        assertTrue(Validators.isValidPassword("securePassword!123"))

        assertFalse(Validators.isValidPassword("        ")) // 8 spaces fails
        assertFalse(Validators.isValidPassword(""))
        assertFalse(Validators.isValidPassword(null))
    }

    @Test
    fun testIsValidLatLon_Double() {
        assertTrue(Validators.isValidLatLon(19.0760, 72.8777))
        assertTrue(Validators.isValidLatLon(-90.0, -180.0))
        assertTrue(Validators.isValidLatLon(90.0, 180.0))
        assertTrue(Validators.isValidLatLon(0.0, 0.0))

        assertFalse(Validators.isValidLatLon(91.0, 0.0))
        assertFalse(Validators.isValidLatLon(-90.1, 0.0))
        assertFalse(Validators.isValidLatLon(0.0, 181.0))
        assertFalse(Validators.isValidLatLon(0.0, -180.1))
        assertFalse(Validators.isValidLatLon(Double.NaN, 0.0))
        assertFalse(Validators.isValidLatLon(0.0, Double.POSITIVE_INFINITY))
        assertFalse(Validators.isValidLatLon(null, 0.0))
        assertFalse(Validators.isValidLatLon(null as Double?, null as Double?))
    }

    @Test
    fun testIsValidLatLon_String() {
        assertTrue(Validators.isValidLatLon("19.0760", "72.8777"))
        assertTrue(Validators.isValidLatLon("-90.0", "180.0"))

        assertFalse(Validators.isValidLatLon("", "1.0"))
        assertFalse(Validators.isValidLatLon("NaN", "1.0"))
        assertFalse(Validators.isValidLatLon("Infinity", "1.0"))
        assertFalse(Validators.isValidLatLon("91.0", "0.0"))
        assertFalse(Validators.isValidLatLon("0.0", "181.0"))
        assertFalse(Validators.isValidLatLon(null as String?, "1.0"))
        assertFalse(Validators.isValidLatLon("1.0", null as String?))
        assertFalse(Validators.isValidLatLon(null as String?, null as String?))
    }
}
