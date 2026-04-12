package com.bounswe2026group8.emergencyhub.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Shared date/time formatting helpers.
 *
 * Uses [DateUtils.getRelativeTimeSpanString] for human-readable relative
 * timestamps (e.g. "5 min. ago", "2 hours ago").
 */
object TimeUtils {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Converts an ISO 8601 timestamp to a human-readable relative string.
     *
     * Handles fractional seconds, trailing "Z", and "+XX:XX" timezone suffixes
     * that the backend may include.
     *
     * @param iso raw timestamp from the API (e.g. "2026-04-10T14:30:00.123456Z")
     * @return relative string like "5 min. ago" or the raw input on failure
     */
    fun timeAgo(iso: String): String {
        return try {
            val trimmed = iso.substringBefore(".").substringBefore("Z").substringBefore("+")
            val millis = isoFormat.parse(trimmed)?.time ?: return iso
            DateUtils.getRelativeTimeSpanString(
                millis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } catch (_: Exception) {
            iso
        }
    }
}
