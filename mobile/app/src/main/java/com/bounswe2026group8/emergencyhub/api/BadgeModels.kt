package com.bounswe2026group8.emergencyhub.api

import com.google.gson.annotations.SerializedName

/**
 * Represents a single user-badge progress entry returned by the badges API.
 * Mirrors the backend UserBadgeSerializer fields.
 */
data class UserBadgeItem(
    val id: Int,
    @SerializedName("badge_name") val badgeName: String,
    @SerializedName("badge_icon") val badgeIcon: String,
    @SerializedName("badge_description") val badgeDescription: String,
    @SerializedName("current_level") val currentLevel: Int,
    @SerializedName("current_progress") val currentProgress: Int,
    @SerializedName("max_level") val maxLevel: Int,
    @SerializedName("next_level_goal") val nextLevelGoal: Int,
    @SerializedName("is_max_level") val isMaxLevel: Boolean
)
