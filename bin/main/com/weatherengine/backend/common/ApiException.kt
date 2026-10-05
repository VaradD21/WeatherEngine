package com.weatherengine.backend.common

import org.springframework.http.HttpStatus

class ApiException(
    val status: HttpStatus,
    override val message: String
) : RuntimeException(message)

data class ErrorResponse(
    val error: String,
    val message: String
)
