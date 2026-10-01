package com.weatherengine.backend.weather

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.PersonaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@RestController
@RequestMapping("/api")
class WeatherController(
    private val weatherCacheService: WeatherCacheService,
    private val personaService: PersonaService,
    private val widgetBuilder: WidgetBuilder
) {

    @GetMapping("/weather")
    fun getWeather(
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double
    ): ResponseEntity<WeatherBundleDto> {
        validateCoordinates(lat, lon)
        val latKey = String.format(Locale.US, "%.2f", lat)
        val lonKey = String.format(Locale.US, "%.2f", lon)
        val bundle = weatherCacheService.getCachedWeatherBundle(latKey, lonKey, lat, lon)
        return ResponseEntity.ok(bundle)
    }

    @GetMapping("/homepage")
    fun getHomepage(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double
    ): ResponseEntity<HomepageResponse> {
        validateCoordinates(lat, lon)

        val widgetCodes = personaService.getUserWidgetCodes(principal.userId)
        if (widgetCodes.isEmpty()) {
            return ResponseEntity.ok(
                HomepageResponse(
                    widgets = emptyList(),
                    message = "No personas selected yet"
                )
            )
        }

        val latKey = String.format(Locale.US, "%.2f", lat)
        val lonKey = String.format(Locale.US, "%.2f", lon)
        val bundle = weatherCacheService.getCachedWeatherBundle(latKey, lonKey, lat, lon)

        val widgets = widgetCodes.map { code ->
            widgetBuilder.buildWidget(code, bundle)
        }

        return ResponseEntity.ok(HomepageResponse(widgets = widgets))
    }

    private fun validateCoordinates(lat: Double, lon: Double) {
        if (lat.isNaN() || lat.isInfinite() || lat < -90.0 || lat > 90.0) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Latitude must be between -90 and 90")
        }
        if (lon.isNaN() || lon.isInfinite() || lon < -180.0 || lon > 180.0) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Longitude must be between -180 and 180")
        }
    }
}
