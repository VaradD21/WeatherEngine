package com.weatherengine.backend.user

import com.weatherengine.backend.auth.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/users/me/health-profile")
class UserHealthProfileController(
    private val healthProfileRepository: UserHealthProfileRepository
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun getProfile(
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<HealthProfileDto> {
        val profile = healthProfileRepository.findById(user.userId).orElseGet {
            UserHealthProfile(userId = user.userId)
        }
        return ResponseEntity.ok(profile.toDto())
    }

    @PutMapping
    @Transactional
    fun updateProfile(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @Valid @RequestBody dto: HealthProfileDto
    ): ResponseEntity<HealthProfileDto> {
        val profile = healthProfileRepository.findById(user.userId).orElseGet {
            UserHealthProfile(userId = user.userId)
        }
        profile.hasAsthma = dto.hasAsthma
        profile.hasAllergies = dto.hasAllergies
        profile.hasSkinSensitivity = dto.hasSkinSensitivity
        profile.aqiThreshold = dto.aqiThreshold
        profile.uvThreshold = dto.uvThreshold
        profile.humidityThreshold = dto.humidityThreshold
        profile.pollenThreshold = dto.pollenThreshold
        profile.alertsEnabled = dto.alertsEnabled
        profile.updatedAt = Instant.now()

        val saved = healthProfileRepository.save(profile)
        return ResponseEntity.ok(saved.toDto())
    }

    private fun UserHealthProfile.toDto(): HealthProfileDto = HealthProfileDto(
        hasAsthma = this.hasAsthma,
        hasAllergies = this.hasAllergies,
        hasSkinSensitivity = this.hasSkinSensitivity,
        aqiThreshold = this.aqiThreshold,
        uvThreshold = this.uvThreshold,
        humidityThreshold = this.humidityThreshold,
        pollenThreshold = this.pollenThreshold,
        alertsEnabled = this.alertsEnabled,
        updatedAt = this.updatedAt
    )
}
