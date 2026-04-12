package com.bounswe2026group8.emergencyhub.util

import com.google.gson.JsonParser

/**
 * Shared API error parsing helpers.
 *
 * The backend returns errors in several shapes:
 *   - `{"detail": "..."}`
 *   - `{"field_name": ["error message", ...]}`
 *
 * This utility tries each pattern and returns the first match,
 * or [fallback] if the body is unparseable.
 */
object ErrorUtils {

    /**
     * Extracts a user-facing error message from a JSON error body.
     *
     * @param body     raw JSON string from [retrofit2.Response.errorBody]
     * @param fallback default message when parsing fails
     * @return the extracted message or [fallback]
     */
    fun parseApiError(body: String?, fallback: String = "Something went wrong."): String {
        if (body.isNullOrBlank()) return fallback
        return try {
            val json = JsonParser.parseString(body).asJsonObject
            // "detail" is the most common shape from DRF
            json.get("detail")?.asString
            // Otherwise grab the first field error
                ?: json.entrySet().firstOrNull()?.value
                    ?.takeIf { it.isJsonArray && it.asJsonArray.size() > 0 }
                    ?.asJsonArray?.get(0)?.asString
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
