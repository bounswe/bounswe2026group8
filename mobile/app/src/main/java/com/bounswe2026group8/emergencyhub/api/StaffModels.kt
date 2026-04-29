package com.bounswe2026group8.emergencyhub.api

import com.google.gson.annotations.SerializedName

// ── Compact user used by /staff/users/ list endpoint ──────────────────────────

data class StaffUserListItem(
    val id: Int,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    val role: String,
    @SerializedName("staff_role") val staffRole: String = "NONE",
    @SerializedName("is_active") val isActive: Boolean = true,
    val hub: Hub?,
    @SerializedName("date_joined") val dateJoined: String? = null,
    @SerializedName("last_login") val lastLogin: String? = null,
)

data class StaffRoleUpdateRequest(
    @SerializedName("staff_role") val staffRole: String,
    val reason: String? = null,
)

data class AccountStatusUpdateRequest(
    @SerializedName("is_active") val isActive: Boolean,
    val reason: String,
)

// ── Forum moderation ──────────────────────────────────────────────────────────

data class ForumModerationAuthor(
    val id: Int,
    val email: String,
    @SerializedName("full_name") val fullName: String? = null,
)

data class ForumModerationPost(
    val id: Int,
    val title: String,
    val content: String? = null,
    val status: String,
    @SerializedName("forum_type") val forumType: String? = null,
    @SerializedName("hub_name") val hubName: String? = null,
    @SerializedName("comment_count") val commentCount: Int = 0,
    val author: ForumModerationAuthor? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class ForumModerationActionRequest(
    val action: String,
    val reason: String? = null,
)

// ── Expertise verification ────────────────────────────────────────────────────

data class ExpertiseVerificationItem(
    val id: Int,
    val field: String,
    @SerializedName("certification_level") val certificationLevel: String,
    @SerializedName("certification_document_url") val certificationDocumentUrl: String? = null,
    @SerializedName("verification_status") val verificationStatus: String = "PENDING",
    @SerializedName("verification_note") val verificationNote: String? = null,
    @SerializedName("reviewed_by_name") val reviewedByName: String? = null,
    val user: ForumModerationAuthor? = null,
)

data class ExpertiseDecisionRequest(
    val status: String,
    val note: String? = null,
)
