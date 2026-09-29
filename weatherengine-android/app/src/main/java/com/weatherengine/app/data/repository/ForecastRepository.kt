package com.weatherengine.app.data.repository

import com.weatherengine.app.core.forecast.ForecastUi
import com.weatherengine.app.data.api.NetworkResult

interface ForecastRepository {
    suspend fun getForecast(
        lat: Double,
        lon: Double,
        locationLabel: String,
        forceRefresh: Boolean = false
    ): NetworkResult<ForecastUi>
}
