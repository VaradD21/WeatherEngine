package com.weatherengine.app.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object ErrorUtils {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun describeApiError(status: Int, body: String?): String {
        if (body.isNullOrBlank()) {
            return defaultStatusMessage(status)
        }

        val trimmed = body.trim()
        val element: JsonElement = try {
            json.parseToJsonElement(trimmed)
        } catch (_: Exception) {
            // Not valid JSON - check if it's HTML or plain text
            return if (trimmed.startsWith("<") && trimmed.contains(">")) {
                defaultStatusMessage(status)
            } else {
                trimmed
            }
        }

        return try {
            when (element) {
                is JsonObject -> parseJsonObjectError(status, element)
                is JsonArray -> parseJsonArrayError(element)
                else -> {
                    val primitive = element.jsonPrimitive.content
                    if (primitive.isNotBlank()) primitive else defaultStatusMessage(status)
                }
            }
        } catch (_: Exception) {
            defaultStatusMessage(status)
        }
    }

    private fun parseJsonObjectError(status: Int, obj: JsonObject): String {
        // Shape 1: { "message": "...", "error": "..." }
        val message = obj["message"]?.jsonPrimitive?.contentOrNull
        val error = obj["error"]?.jsonPrimitive?.contentOrNull

        if (!message.isNullOrBlank()) {
            return message
        }
        if (!error.isNullOrBlank()) {
            return error
        }

        // Shape 2: { "errors": [...] or {...} }
        val errorsElement = obj["errors"]
        if (errorsElement is JsonArray) {
            val list = parseJsonArrayError(errorsElement)
            if (list.isNotBlank()) return list
        } else if (errorsElement is JsonObject) {
            val fieldErrors = formatMap(errorsElement)
            if (fieldErrors.isNotBlank()) return fieldErrors
        }

        // Shape 3: map of field -> message: { "email": "invalid", "password": "short" }
        val mapEntries = formatMap(obj)
        if (mapEntries.isNotBlank()) {
            return mapEntries
        }

        return defaultStatusMessage(status)
    }

    private fun parseJsonArrayError(array: JsonArray): String {
        val messages = array.mapNotNull { item ->
            when (item) {
                is JsonObject -> {
                    item["defaultMessage"]?.jsonPrimitive?.contentOrNull
                        ?: item["message"]?.jsonPrimitive?.contentOrNull
                        ?: item["error"]?.jsonPrimitive?.contentOrNull
                }
                else -> item.jsonPrimitive.contentOrNull
            }
        }.filter { it.isNotBlank() }

        return if (messages.isNotEmpty()) {
            messages.joinToString(", ")
        } else {
            ""
        }
    }

    private fun formatMap(obj: JsonObject): String {
        val pairs = obj.entries.mapNotNull { (key, value) ->
            val msg = value.jsonPrimitive.contentOrNull
            if (!msg.isNullOrBlank()) "$key: $msg" else null
        }
        return pairs.joinToString(", ")
    }

    private fun defaultStatusMessage(status: Int): String = when (status) {
        400 -> "Invalid request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not found"
        409 -> "Email already registered"
        500 -> "Server error"
        502, 503, 504 -> "Server unavailable"
        else -> "Error ($status)"
    }
}

