package com.weatherengine.app.data.api

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Failure(val kind: FailureKind, val message: String) : NetworkResult<Nothing>()
}

enum class FailureKind {
    Network,
    Timeout,
    Unauthorized,
    Client,
    Server,
    Parse
}
