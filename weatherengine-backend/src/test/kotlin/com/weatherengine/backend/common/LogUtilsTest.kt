package com.weatherengine.backend.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LogUtilsTest {

    @Test
    fun `roundForLog formats coordinates to 1 decimal place`() {
        assertEquals("19.1", 19.0752.roundForLog())
        assertEquals("72.9", 72.8777.roundForLog())
        assertEquals("0.0", 0.04.roundForLog())
        assertEquals("-12.3", (-12.34).roundForLog())
        assertEquals("-12.4", (-12.36).roundForLog())
    }
}
