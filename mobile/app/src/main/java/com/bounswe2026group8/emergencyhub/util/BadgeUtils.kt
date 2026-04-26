package com.bounswe2026group8.emergencyhub.util

import android.content.Context
import com.bounswe2026group8.emergencyhub.R

/**
 * Shared color mappings and localized labels for badges.
 */
object BadgeUtils {

    fun categoryColors(category: String): Pair<Int, Int> = when (category) {
        "MEDICAL" -> R.color.category_medical to R.color.category_medical_bg
        "FOOD" -> R.color.category_food to R.color.category_food_bg
        "SHELTER" -> R.color.category_shelter to R.color.category_shelter_bg
        "TRANSPORT" -> R.color.category_transport to R.color.category_transport_bg
        else -> R.color.text_secondary to R.color.badge_muted_bg
    }

    fun urgencyColors(urgency: String): Pair<Int, Int> = when (urgency) {
        "HIGH" -> R.color.urgency_high to R.color.urgency_high_bg
        "MEDIUM" -> R.color.urgency_medium to R.color.urgency_medium_bg
        else -> R.color.urgency_low to R.color.urgency_low_bg
    }

    fun formatCategoryLabel(context: Context, raw: String): String = when (raw.uppercase()) {
        "MEDICAL" -> context.getString(R.string.category_medical)
        "FOOD" -> context.getString(R.string.category_food)
        "SHELTER" -> context.getString(R.string.category_shelter)
        "TRANSPORT" -> context.getString(R.string.category_transport)
        else -> formatFallbackLabel(raw)
    }

    fun formatUrgencyLabel(context: Context, raw: String): String = when (raw.uppercase()) {
        "LOW" -> context.getString(R.string.urgency_low)
        "MEDIUM" -> context.getString(R.string.urgency_medium)
        "HIGH" -> context.getString(R.string.urgency_high)
        else -> formatFallbackLabel(raw)
    }

    fun formatStatusLabel(context: Context, raw: String): String = when (raw.uppercase()) {
        "EXPERT_RESPONDING" -> context.getString(R.string.status_expert_responding)
        "RESOLVED" -> context.getString(R.string.status_resolved)
        else -> context.getString(R.string.status_open)
    }

    fun formatAvailabilityLabel(context: Context, raw: String): String = when (raw.trim().uppercase()) {
        "24/7" -> context.getString(R.string.availability_247)
        "WEEKDAYS" -> context.getString(R.string.availability_weekdays)
        "WEEKENDS" -> context.getString(R.string.availability_weekends)
        "MORNINGS" -> context.getString(R.string.availability_mornings)
        "EVENINGS" -> context.getString(R.string.availability_evenings)
        "ON-CALL", "ON_CALL" -> context.getString(R.string.availability_oncall)
        else -> raw
    }

    private fun formatFallbackLabel(raw: String): String =
        raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}
