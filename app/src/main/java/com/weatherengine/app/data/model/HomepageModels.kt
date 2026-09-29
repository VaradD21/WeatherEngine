package com.weatherengine.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HomepageResponse(
    val widgets: List<WidgetDto> = emptyList(),
    val message: String? = null
)

@Serializable
data class WidgetDto(
    val type: String,
    val data: JsonElement
)
