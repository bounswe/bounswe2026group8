package com.bounswe2026group8.emergencyhub.ui

import android.content.Context
import com.bounswe2026group8.emergencyhub.R

object BadgeLocalizer {
    fun getLocalizedBadgeName(context: Context, badgeName: String): String {
        return when (badgeName) {
            "Commenter" -> context.getString(R.string.badge_name_commenter)
            "Voter" -> context.getString(R.string.badge_name_voter)
            "Forum Active" -> context.getString(R.string.badge_name_forum_active)
            "Responder" -> context.getString(R.string.badge_name_responder)
            else -> badgeName
        }
    }

    fun getLocalizedBadgeDescription(context: Context, badgeName: String, originalDescription: String): String {
        return when (badgeName) {
            "Commenter" -> context.getString(R.string.badge_desc_commenter)
            "Voter" -> context.getString(R.string.badge_desc_voter)
            "Forum Active" -> context.getString(R.string.badge_desc_forum_active)
            "Responder" -> context.getString(R.string.badge_desc_responder)
            else -> originalDescription
        }
    }
}
