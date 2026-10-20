package com.weatherengine.backend.user

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.weather.ParentPersonaRules
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/users/me/preferences")
class UserPreferencesController(
    private val preferencesRepository: UserPreferencesRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun getPreferences(
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<UserPreferencesDto> {
        val prefs = preferencesRepository.findById(user.userId).orElseGet {
            UserPreferences(userId = user.userId)
        }
        return ResponseEntity.ok(prefs.toDto())
    }

    @PutMapping
    @Transactional
    fun updatePreferences(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @Valid @RequestBody dto: UserPreferencesDto
    ): ResponseEntity<UserPreferencesDto> {
        // Validate school hours BEFORE touching the DB.
        // ParentPersonaRules.parseAndValidateSchoolHours throws IllegalArgumentException
        // with a clear message on bad format or end <= start.
        try {
            ParentPersonaRules.parseAndValidateSchoolHours(dto.schoolStart, dto.schoolEnd)
        } catch (ex: IllegalArgumentException) {
            throw ApiException(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid school hours")
        }

        // Validate enum values and widget codes.
        try {
            dto.validateBusinessRules()
        } catch (ex: IllegalArgumentException) {
            throw ApiException(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid preferences value")
        }

        val prefs = preferencesRepository.findById(user.userId).orElseGet {
            UserPreferences(userId = user.userId)
        }
        prefs.widgetOrder = dto.widgetOrder.filter { it.isNotBlank() }.joinToString(",")
        prefs.disabledWidgets = dto.disabledWidgets.filter { it.isNotBlank() }.joinToString(",")
        prefs.defaultScreen = dto.defaultScreen.ifBlank { "home" }
        prefs.temperatureUnit = if (dto.temperatureUnit.equals("FAHRENHEIT", ignoreCase = true)) "FAHRENHEIT" else "CELSIUS"
        // Hours are guaranteed valid at this point.
        prefs.schoolStart = dto.schoolStart
        prefs.schoolEnd = dto.schoolEnd
        prefs.updatedAt = Instant.now()

        val saved = preferencesRepository.save(prefs)
        return ResponseEntity.ok(saved.toDto())
    }

    private fun UserPreferences.toDto(): UserPreferencesDto {
        val order = this.widgetOrder?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val disabled = this.disabledWidgets?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        return UserPreferencesDto(
            widgetOrder = order,
            disabledWidgets = disabled,
            defaultScreen = this.defaultScreen,
            temperatureUnit = this.temperatureUnit,
            schoolStart = this.schoolStart,
            schoolEnd = this.schoolEnd,
            updatedAt = this.updatedAt
        )
    }
}
