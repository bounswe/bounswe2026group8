package com.bounswe2026group8.emergencyhub.util

import com.bounswe2026group8.emergencyhub.R

/**
 * Shared color mappings for category and urgency badges.
 *
 * Returns pairs of (textColor, backgroundColor) resource IDs
 * used across list adapters and detail screens.
 */
object BadgeUtils {

    /** Returns (textColor, backgroundColor) resource IDs for a help-request category. */
    fun categoryColors(category: String): Pair<Int, Int> = when (category) {
        "MEDICAL"   -> R.color.category_medical   to R.color.category_medical_bg
        "FOOD"      -> R.color.category_food       to R.color.category_food_bg
        "SHELTER"   -> R.color.category_shelter    to R.color.category_shelter_bg
        "TRANSPORT" -> R.color.category_transport  to R.color.category_transport_bg
        else        -> R.color.text_secondary      to R.color.badge_muted_bg
    }

    /** Returns (textColor, backgroundColor) resource IDs for an urgency level. */
    fun urgencyColors(urgency: String): Pair<Int, Int> = when (urgency) {
        "HIGH"   -> R.color.urgency_high   to R.color.urgency_high_bg
        "MEDIUM" -> R.color.urgency_medium to R.color.urgency_medium_bg
        else     -> R.color.urgency_low    to R.color.urgency_low_bg
    }

    /** Capitalizes a raw enum value (e.g. "MEDICAL" → "Medical"). */
    fun formatLabel(raw: String): String = raw.lowercase().replaceFirstChar { it.uppercase() }
}
