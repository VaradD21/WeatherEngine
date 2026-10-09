package com.weatherengine.backend.weather

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.PersonaService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class ParentPersonaRulesTest {

    private val start8 = LocalTime.of(8, 0)
    private val end15 = LocalTime.of(15, 0)

    // ==========================================
    // 1. nextSchoolRun tests
    // ==========================================
    @Test
    fun `nextSchoolRun weekday before morning window selects today morning`() {
        val now = LocalDateTime.of(2026, 10, 14, 6, 30) // Wednesday 06:30
        val run = ParentPersonaRules.nextSchoolRun(now, start8, end15)

        assertEquals("morning", run.run)
        assertEquals(LocalDate.of(2026, 10, 14), run.date)
        assertEquals(LocalDateTime.of(2026, 10, 14, 7, 0), run.windowStart)
        assertEquals(LocalDateTime.of(2026, 10, 14, 8, 0), run.windowEnd)
    }

    @Test
    fun `nextSchoolRun inside morning window selects today morning`() {
        val now = LocalDateTime.of(2026, 10, 14, 7, 30) // Wednesday 07:30
        val run = ParentPersonaRules.nextSchoolRun(now, start8, end15)

        assertEquals("morning", run.run)
        assertEquals(LocalDate.of(2026, 10, 14), run.date)
    }

    @Test
    fun `nextSchoolRun between windows selects today afternoon`() {
        val now = LocalDateTime.of(2026, 10, 14, 10, 0) // Wednesday 10:00
        val run = ParentPersonaRules.nextSchoolRun(now, start8, end15)

        assertEquals("afternoon", run.run)
        assertEquals(LocalDate.of(2026, 10, 14), run.date)
        assertEquals(LocalDateTime.of(2026, 10, 14, 15, 0), run.windowStart)
        assertEquals(LocalDateTime.of(2026, 10, 14, 16, 0), run.windowEnd)
    }

    @Test
    fun `nextSchoolRun inside afternoon window selects today afternoon`() {
        val now = LocalDateTime.of(2026, 10, 14, 15, 30) // Wednesday 15:30
        val run = ParentPersonaRules.nextSchoolRun(now, start8, end15)

        assertEquals("afternoon", run.run)
        assertEquals(LocalDate.of(2026, 10, 14), run.date)
    }

    @Test
    fun `nextSchoolRun after afternoon window selects tomorrow morning`() {
        val now = LocalDateTime.of(2026, 10, 14, 16, 1) // Wednesday 16:01
        val run = ParentPersonaRules.nextSchoolRun(now, start8, end15)

        assertEquals("morning", run.run)
        assertEquals(LocalDate.of(2026, 10, 15), run.date) // Thursday
    }

    @Test
    fun `nextSchoolRun Friday evening rolls over to Monday morning`() {
        val now = LocalDateTime.of(2026, 10, 16, 17, 0) // Friday 17:00
        val run = ParentPersonaRules.nextSchoolRun(now, start8, end15)

        assertEquals("morning", run.run)
        assertEquals(DayOfWeek.MONDAY, run.date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 19), run.date)
    }

    @Test
    fun `nextSchoolRun Saturday and Sunday roll over to Monday morning`() {
        val sat = LocalDateTime.of(2026, 10, 17, 10, 0) // Saturday
        val runSat = ParentPersonaRules.nextSchoolRun(sat, start8, end15)
        assertEquals("morning", runSat.run)
        assertEquals(DayOfWeek.MONDAY, runSat.date.dayOfWeek)

        val sun = LocalDateTime.of(2026, 10, 18, 14, 0) // Sunday
        val runSun = ParentPersonaRules.nextSchoolRun(sun, start8, end15)
        assertEquals("morning", runSun.run)
        assertEquals(DayOfWeek.MONDAY, runSun.date.dayOfWeek)
    }

    @Test
    fun `nextSchoolRun near midnight behaves correctly`() {
        val lateNight = LocalDateTime.of(2026, 10, 14, 23, 55) // Wednesday late night
        val runLate = ParentPersonaRules.nextSchoolRun(lateNight, start8, end15)
        assertEquals("morning", runLate.run)
        assertEquals(LocalDate.of(2026, 10, 15), runLate.date)

        val earlyMorning = LocalDateTime.of(2026, 10, 14, 0, 5) // Wednesday just past midnight
        val runEarly = ParentPersonaRules.nextSchoolRun(earlyMorning, start8, end15)
        assertEquals("morning", runEarly.run)
        assertEquals(LocalDate.of(2026, 10, 14), runEarly.date)
    }

    @Test
    fun `school hours validation rejects start equals end or end before start and bad strings`() {
        assertThrows(IllegalArgumentException::class.java) {
            ParentPersonaRules.parseAndValidateSchoolHours("08:00", "08:00")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ParentPersonaRules.parseAndValidateSchoolHours("15:00", "08:00")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ParentPersonaRules.parseAndValidateSchoolHours("invalid", "15:00")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ParentPersonaRules.parseAndValidateSchoolHours("08:00", "25:00")
        }
    }

    // ==========================================
    // 2. Verdict boundaries & worst-wins tests
    // ==========================================
    private fun makeHourly(
        times: List<String> = listOf("2026-10-14T07:00", "2026-10-14T08:00"),
        rainProb: List<Int?> = listOf(0, 0),
        precip: List<Double?> = listOf(0.0, 0.0),
        gusts: List<Double?> = listOf(10.0, 10.0),
        visibility: List<Double?> = listOf(10000.0, 10000.0),
        apparent: List<Double?> = listOf(20.0, 20.0),
        uv: List<Double?> = listOf(2.0, 2.0),
        code: List<Int?> = listOf(0, 0)
    ) = OpenMeteoHourlyForecast(
        time = times,
        precipitationProbability = rainProb,
        precipitation = precip,
        windGusts10m = gusts,
        visibility = visibility,
        apparentTemperature = apparent,
        uvIndex = uv,
        weatherCode = code
    )

    private val morningWindow = SchoolRunWindow(
        run = "morning",
        date = LocalDate.of(2026, 10, 14),
        windowStart = LocalDateTime.of(2026, 10, 14, 7, 0),
        windowEnd = LocalDateTime.of(2026, 10, 14, 8, 0)
    )

    private val afternoonWindow = SchoolRunWindow(
        run = "afternoon",
        date = LocalDate.of(2026, 10, 14),
        windowStart = LocalDateTime.of(2026, 10, 14, 15, 0),
        windowEnd = LocalDateTime.of(2026, 10, 14, 16, 0)
    )

    @Test
    fun `precipitation boundary 0_4 vs 0_5 and 7_4 vs 7_5`() {
        val h04 = makeHourly(precip = listOf(0.4, 0.0))
        assertEquals("good", ParentPersonaRules.computeSchoolCommuteCard(h04, morningWindow)["verdict"])

        val h05 = makeHourly(precip = listOf(0.5, 0.0))
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(h05, morningWindow)["verdict"])

        val h74 = makeHourly(precip = listOf(7.4, 0.0))
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(h74, morningWindow)["verdict"])

        val h75 = makeHourly(precip = listOf(7.5, 0.0))
        assertEquals("caution", ParentPersonaRules.computeSchoolCommuteCard(h75, morningWindow)["verdict"])
    }

    @Test
    fun `rain probability boundary 39 vs 40`() {
        val h39 = makeHourly(rainProb = listOf(39, 0))
        assertEquals("good", ParentPersonaRules.computeSchoolCommuteCard(h39, morningWindow)["verdict"])

        val h40 = makeHourly(rainProb = listOf(40, 0))
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(h40, morningWindow)["verdict"])
    }

    @Test
    fun `gust boundary 29 vs 30 and 49 vs 50`() {
        val h29 = makeHourly(gusts = listOf(29.0, 10.0))
        assertEquals("good", ParentPersonaRules.computeSchoolCommuteCard(h29, morningWindow)["verdict"])

        val h30 = makeHourly(gusts = listOf(30.0, 10.0))
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(h30, morningWindow)["verdict"])

        val h49 = makeHourly(gusts = listOf(49.0, 10.0))
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(h49, morningWindow)["verdict"])

        val h50 = makeHourly(gusts = listOf(50.0, 10.0))
        assertEquals("caution", ParentPersonaRules.computeSchoolCommuteCard(h50, morningWindow)["verdict"])
    }

    @Test
    fun `visibility boundary 999 vs 1000`() {
        val h999 = makeHourly(visibility = listOf(999.0, 10000.0))
        assertEquals("caution", ParentPersonaRules.computeSchoolCommuteCard(h999, morningWindow)["verdict"])

        val h1000 = makeHourly(visibility = listOf(1000.0, 10000.0))
        assertEquals("good", ParentPersonaRules.computeSchoolCommuteCard(h1000, morningWindow)["verdict"])
    }

    @Test
    fun `apparent temp boundaries`() {
        // Hot boundaries: 31.9 (good), 32.0 (prepare), 37.9 (prepare), 38.0 (caution)
        assertEquals("good", ParentPersonaRules.computeSchoolCommuteCard(makeHourly(apparent = listOf(31.9, 20.0)), morningWindow)["verdict"])
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(makeHourly(apparent = listOf(32.0, 20.0)), morningWindow)["verdict"])
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(makeHourly(apparent = listOf(37.9, 20.0)), morningWindow)["verdict"])
        assertEquals("caution", ParentPersonaRules.computeSchoolCommuteCard(makeHourly(apparent = listOf(38.0, 20.0)), morningWindow)["verdict"])

        // Cold boundaries: 8.1 (good), 8.0 (prepare), 0.1 (prepare), 0.0 (caution)
        assertEquals("good", ParentPersonaRules.computeSchoolCommuteCard(makeHourly(apparent = listOf(8.1, 20.0)), morningWindow)["verdict"])
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(makeHourly(apparent = listOf(8.0, 20.0)), morningWindow)["verdict"])
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(makeHourly(apparent = listOf(0.1, 20.0)), morningWindow)["verdict"])
        assertEquals("caution", ParentPersonaRules.computeSchoolCommuteCard(makeHourly(apparent = listOf(0.0, 20.0)), morningWindow)["verdict"])
    }

    @Test
    fun `uv index boundary 5 vs 6 only affects afternoon run`() {
        val h5 = makeHourly(
            times = listOf("2026-10-14T15:00", "2026-10-14T16:00"),
            uv = listOf(5.0, 2.0)
        )
        assertEquals("good", ParentPersonaRules.computeSchoolCommuteCard(h5, afternoonWindow)["verdict"])

        val h6 = makeHourly(
            times = listOf("2026-10-14T15:00", "2026-10-14T16:00"),
            uv = listOf(6.0, 2.0)
        )
        assertEquals("prepare", ParentPersonaRules.computeSchoolCommuteCard(h6, afternoonWindow)["verdict"])

        // Morning run ignores UV
        val h6Morning = makeHourly(uv = listOf(6.0, 2.0))
        assertEquals("good", ParentPersonaRules.computeSchoolCommuteCard(h6Morning, morningWindow)["verdict"])
    }

    @Test
    fun `thunderstorm codes trigger caution and worst wins`() {
        for (code in listOf(95, 96, 99)) {
            val h = makeHourly(code = listOf(code, 0))
            assertEquals("caution", ParentPersonaRules.computeSchoolCommuteCard(h, morningWindow)["verdict"])
        }

        // Worst wins: caution condition (e.g. gusts 55) overrides prepare condition (rain prob 40)
        val hMixed = makeHourly(rainProb = listOf(40, 0), gusts = listOf(55.0, 10.0))
        assertEquals("caution", ParentPersonaRules.computeSchoolCommuteCard(hMixed, morningWindow)["verdict"])
    }

    @Test
    fun `null metrics are skipped and empty hourly yields unavailable`() {
        val hNull = makeHourly(
            rainProb = listOf(null, null),
            precip = listOf(null, null),
            gusts = listOf(null, null),
            visibility = listOf(null, null),
            apparent = listOf(null, null)
        )
        val res = ParentPersonaRules.computeSchoolCommuteCard(hNull, morningWindow)
        assertEquals("good", res["verdict"])

        val empty = OpenMeteoHourlyForecast()
        val resEmpty = ParentPersonaRules.computeSchoolCommuteCard(empty, morningWindow)
        assertEquals("unavailable", resEmpty["status"])
    }

    // ==========================================
    // 3. rain_alert tests
    // ==========================================
    @Test
    fun `rain_alert peak hour and thresholds 19, 20, 49, 50`() {
        val now = LocalDateTime.of(2026, 10, 14, 12, 0)
        val times = (12..17).map { "2026-10-14T$it:00" }

        // 19 -> No rain expected
        val h19 = OpenMeteoHourlyForecast(time = times, precipitationProbability = listOf(19, 10, 5, 0, 0, 0))
        val r19 = ParentPersonaRules.computeRainAlertCard(h19, now)
        assertEquals("No rain expected in the next 6 hours", r19["message"])
        assertEquals(19, r19["peakProbabilityPercent"])

        // 20 -> Light chance of rain
        val h20 = OpenMeteoHourlyForecast(time = times, precipitationProbability = listOf(10, 20, 5, 0, 0, 0))
        val r20 = ParentPersonaRules.computeRainAlertCard(h20, now)
        assertEquals("Light chance of rain", r20["message"])
        assertEquals(20, r20["peakProbabilityPercent"])

        // 49 -> Light chance of rain
        val h49 = OpenMeteoHourlyForecast(time = times, precipitationProbability = listOf(10, 49, 5, 0, 0, 0))
        val r49 = ParentPersonaRules.computeRainAlertCard(h49, now)
        assertEquals("Light chance of rain", r49["message"])

        // 50 at 2 PM (index 2: 14:00) -> Rain likely around 2 PM
        val h50 = OpenMeteoHourlyForecast(time = times, precipitationProbability = listOf(10, 30, 50, 20, 0, 0))
        val r50 = ParentPersonaRules.computeRainAlertCard(h50, now)
        assertEquals("Rain likely around 2 PM", r50["message"])
        assertEquals(50, r50["peakProbabilityPercent"])

        // Empty list -> unavailable
        val rEmpty = ParentPersonaRules.computeRainAlertCard(OpenMeteoHourlyForecast(), now)
        assertEquals("unavailable", rEmpty["status"])
    }

    // ==========================================
    // 4. severe_weather tests
    // ==========================================
    @Test
    fun `severe_weather alerts individually and combined`() {
        val now = LocalDateTime.of(2026, 10, 14, 10, 0)
        val times = (10..33).map {
            val d = if (it < 24) "14" else "15"
            val h = String.format("%02d", it % 24)
            "2026-10-${d}T$h:00"
        }

        // None
        val hNone = OpenMeteoHourlyForecast(time = times)
        val rNone = ParentPersonaRules.computeSevereWeatherCard(hNone, now)
        assertEquals("ok", rNone["status"])
        assertTrue((rNone["message"] as String).contains("No severe weather expected"))
        assertTrue((rNone["message"] as String).contains("App estimate, not an official IMD warning"))
        assertEquals(emptyList<Any>(), rNone["alerts"])

        // Thunderstorm at 1 PM (13:00, index 3)
        val hThunder = OpenMeteoHourlyForecast(
            time = times,
            weatherCode = times.indices.map { if (it == 3) 95 else 0 }
        )
        val rThunder = ParentPersonaRules.computeSevereWeatherCard(hThunder, now)
        @Suppress("UNCHECKED_CAST")
        val alertsThunder = rThunder["alerts"] as List<Map<String, String>>
        assertEquals(1, alertsThunder.size)
        assertEquals("thunderstorm", alertsThunder[0]["type"])
        assertEquals("1 PM", alertsThunder[0]["startsAt"])

        // Several at once (Thunderstorm at index 3, Extreme Heat at index 4)
        val hMulti = OpenMeteoHourlyForecast(
            time = times,
            weatherCode = times.indices.map { if (it == 3) 95 else 0 },
            apparentTemperature = times.indices.map { if (it == 4) 41.0 else 25.0 }
        )
        val rMulti = ParentPersonaRules.computeSevereWeatherCard(hMulti, now)
        @Suppress("UNCHECKED_CAST")
        val alertsMulti = rMulti["alerts"] as List<Map<String, String>>
        assertEquals(2, alertsMulti.size)
        assertEquals("thunderstorm", alertsMulti[0]["type"])
        assertEquals("extreme_heat", alertsMulti[1]["type"])
        assertEquals("2 PM", alertsMulti[1]["startsAt"])
    }

    // ==========================================
    // 5. Controller & WidgetBuilder wiring tests
    // ==========================================
    @Test
    fun `WidgetBuilder builds all three parent cards`() {
        val builder = WidgetBuilder()
        val today = java.time.LocalDate.now(java.time.ZoneId.of("UTC"))
        val hourlyTimes = (0 until 168).map { today.atStartOfDay().plusHours(it.toLong()).toString() }
        val bundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                timezone = "UTC",
                hourly = OpenMeteoHourlyForecast(
                    time = hourlyTimes,
                    precipitationProbability = List(168) { 10 },
                    precipitation = List(168) { 0.0 },
                    windGusts10m = List(168) { 15.0 },
                    visibility = List(168) { 10000.0 },
                    apparentTemperature = List(168) { 22.0 }
                )
            )
        )

        val commute = builder.buildWidget("school_commute_card", bundle)
        assertEquals("school_commute_card", commute.type)
        assertEquals("ok", commute.data["status"])
        assertNotNull(commute.data["verdict"])
        assertNotNull(commute.data["reasons"])
        assertNotNull(commute.data["tips"])

        val rain = builder.buildWidget("rain_alert_card", bundle)
        assertEquals("rain_alert_card", rain.type)
        assertEquals("ok", rain.data["status"])
        assertNotNull(rain.data["message"])

        val severe = builder.buildWidget("severe_weather_card", bundle)
        assertEquals("severe_weather_card", severe.type)
        assertEquals("ok", severe.data["status"])
        assertNotNull(severe.data["message"])
    }

    @Test
    fun `WeatherController validates schoolStart and schoolEnd params`() {
        val cacheService = mockk<WeatherCacheService>()
        val personaService = mockk<PersonaService>()
        val widgetBuilder = WidgetBuilder()
        val controller = WeatherController(cacheService, personaService, widgetBuilder)

        val principal = AuthenticatedUser(UUID.randomUUID(), "test@example.com")

        // Bad format
        val ex1 = assertThrows(ApiException::class.java) {
            controller.getHomepage(principal, 28.0, 77.0, "8am", "15:00")
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex1.status)

        // End before start
        val ex2 = assertThrows(ApiException::class.java) {
            controller.getHomepage(principal, 28.0, 77.0, "16:00", "15:00")
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex2.status)

        // Start equals end
        val ex3 = assertThrows(ApiException::class.java) {
            controller.getHomepage(principal, 28.0, 77.0, "08:00", "08:00")
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex3.status)
    }

    @Test
    fun `WeatherController returns widgets only for active user personas`() {
        val cacheService = mockk<WeatherCacheService>()
        val personaService = mockk<PersonaService>()
        val widgetBuilder = WidgetBuilder()
        val controller = WeatherController(cacheService, personaService, widgetBuilder)

        val userId = UUID.randomUUID()
        val principal = AuthenticatedUser(userId, "parent@example.com")

        every { personaService.getUserWidgetCodes(userId) } returns listOf(
            "school_commute_card",
            "rain_alert_card",
            "severe_weather_card"
        )
        every { cacheService.getCachedWeatherBundle(28.0, 77.0) } returns WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                timezone = "UTC",
                hourly = OpenMeteoHourlyForecast(
                    time = (0..23).map { "2026-10-14T${String.format("%02d", it)}:00" },
                    precipitationProbability = List(24) { 10 },
                    precipitation = List(24) { 0.0 },
                    windGusts10m = List(24) { 10.0 },
                    visibility = List(24) { 10000.0 },
                    apparentTemperature = List(24) { 20.0 }
                )
            )
        )

        val response = controller.getHomepage(principal, 28.0, 77.0, "08:00", "15:00")
        assertEquals(HttpStatus.OK, response.statusCode)
        val widgets = response.body?.widgets
        assertNotNull(widgets)
        assertEquals(3, widgets?.size)
        assertEquals(listOf("school_commute_card", "rain_alert_card", "severe_weather_card"), widgets?.map { it.type })
    }

    // ==========================================
    // 6. Cache schema bump test
    // ==========================================
    @Test
    fun `bundle schema bump does not break parsing of an old cached entry without precipitation`() {
        val mapper = ObjectMapper().registerKotlinModule()
        // Simulate old cached JSON payload lacking the "precipitation" field in hourly
        val oldJson = """
        {
            "forecast": {
                "latitude": 28.0,
                "longitude": 77.0,
                "hourly": {
                    "time": ["2026-10-14T08:00"],
                    "temperature_2m": [25.0],
                    "apparent_temperature": [26.0],
                    "precipitation_probability": [20],
                    "wind_speed_10m": [10.0]
                }
            },
            "fetchedAtMs": 1700000000000
        }
        """.trimIndent()

        val parsed = mapper.readValue(oldJson, WeatherBundleDto::class.java)
        assertNotNull(parsed)
        assertEquals(28.0, parsed.forecast.latitude)
        assertNotNull(parsed.forecast.hourly)
        assertTrue(parsed.forecast.hourly?.precipitation?.isEmpty() == true)
    }
}
