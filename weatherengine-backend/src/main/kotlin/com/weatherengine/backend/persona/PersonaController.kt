package com.weatherengine.backend.persona

import com.weatherengine.backend.auth.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/me/personas")
class PersonaController(
    private val personaService: PersonaService
) {

    @GetMapping
    fun getPersonas(
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): ResponseEntity<List<PersonaDto>> {
        return ResponseEntity.ok(personaService.getUserPersonas(principal.userId))
    }

    @PostMapping
    fun setPersonas(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: SetPersonasRequest
    ): ResponseEntity<List<PersonaDto>> {
        val updated = personaService.setUserPersonas(principal.userId, request.personaCodes)
        return ResponseEntity.ok(updated)
    }
}
