package com.bounswe2026group8.emergencyhub.util

/**
 * Resolves image URLs so relative paths served by the backend are loadable.
 *
 * Extracted from RetrofitClient so the logic can be unit-tested without
 * requiring BuildConfig or Android framework dependencies.
 *
 * - Relative paths (starting with "/") get the base URL prepended.
 * - Absolute URLs (http/https) are returned unchanged.
 */
object ImageUrlResolver {

    /**
     * @param url     raw URL string from the API response
     * @param baseUrl the server base URL (e.g. "http://10.0.2.2:8000/")
     * @return        a fully-qualified URL loadable by image loading libraries
     */
    fun resolve(url: String, baseUrl: String): String {
        if (url.startsWith("/")) {
            // Strip any API path prefix (e.g. "/api") so media files at the server
            // root (e.g. /media/uploads/…) are resolved correctly.
            val origin = baseUrl.trimEnd('/').let { base ->
                val apiIndex = base.indexOf("/api")
                if (apiIndex != -1) base.substring(0, apiIndex) else base
            }
            return origin + url
        }
        return url
    }
}
