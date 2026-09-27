package com.weatherengine.backend.config

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class TestAuthController {
    @GetMapping("/test")
    fun testAuth(): Map<String, String> = mapOf("status" to "public-auth-success")
}

@RestController
@RequestMapping("/api/data")
class TestApiController {
    @GetMapping("/test")
    fun testData(): Map<String, String> = mapOf("status" to "protected-api-success")
}

@RestController
@RequestMapping("/other")
class TestOtherController {
    @GetMapping("/unprotected")
    fun testOther(): Map<String, String> = mapOf("status" to "other-resource")
}
