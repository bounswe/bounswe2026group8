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
        if (url.startsWith("/")) return baseUrl.trimEnd('/') + url
        return url
    }
}
