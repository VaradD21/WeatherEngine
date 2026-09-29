package com.weatherengine.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun testNormalizeBaseUrl() {
        assertNull(UrlUtils.normalizeBaseUrl(""))
        assertNull(UrlUtils.normalizeBaseUrl("   "))
        assertNull(UrlUtils.normalizeBaseUrl(null))

        assertEquals("http://localhost:8080", UrlUtils.normalizeBaseUrl("http://localhost:8080/"))
        assertEquals("http://localhost:8080", UrlUtils.normalizeBaseUrl("http://localhost:8080///"))
        assertEquals("https://api.weatherengine.com", UrlUtils.normalizeBaseUrl("https://api.weatherengine.com/"))
        assertEquals("https://x.up.railway.app", UrlUtils.normalizeBaseUrl("https://x.up.railway.app///"))

        assertNull(UrlUtils.normalizeBaseUrl("javascript:alert(1)"))
        assertNull(UrlUtils.normalizeBaseUrl("ftp://files.example.com"))
        assertNull(UrlUtils.normalizeBaseUrl("localhost:8080")) // missing scheme
        assertNull(UrlUtils.normalizeBaseUrl("http://"))
        assertNull(UrlUtils.normalizeBaseUrl("https:///"))
    }
}
