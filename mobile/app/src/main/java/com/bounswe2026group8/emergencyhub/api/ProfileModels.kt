package com.bounswe2026group8.emergencyhub.api

import com.google.gson.annotations.SerializedName

// ── Profile ─────────────────────────────────────────────────────────────────────

data class ProfileData(
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("blood_type") val bloodType: String?,
    @SerializedName("emergency_contact_phone") val emergencyContactPhone: String?,
    @SerializedName("special_needs") val specialNeeds: String?,
    @SerializedName("has_disability") val hasDisability: Boolean,
    @SerializedName("availability_status") val availabilityStatus: String,
    val bio: String?,
    @SerializedName("preferred_language") val preferredLanguage: String?,
    @SerializedName("emergency_contact") val emergencyContact: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
)

data class UserPublicProfileData(
    val id: Int,
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val role: String,
    @SerializedName("staff_role") val staffRole: String = "NONE",
    val hub: Hub?,
    @SerializedName("neighborhood_address") val neighborhoodAddress: String?,
    val profile: ProfileData?,
    val resources: List<ResourceData>?,
    @SerializedName("expertise_fields") val expertiseFields: List<ExpertiseFieldData>?
)

data class ProfileUpdateRequest(
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("blood_type") val bloodType: String? = null,
    @SerializedName("emergency_contact_phone") val emergencyContactPhone: String? = null,
    @SerializedName("special_needs") val specialNeeds: String? = null,
    @SerializedName("has_disability") val hasDisability: Boolean? = null,
    @SerializedName("availability_status") val availabilityStatus: String? = null,
    val bio: String? = null,
    @SerializedName("preferred_language") val preferredLanguage: String? = null,
    @SerializedName("emergency_contact") val emergencyContact: String? = null,
)

// Settings

data class UserSettingsData(
    @SerializedName("notify_help_requests") val notifyHelpRequests: Boolean,
    @SerializedName("notify_urgent_posts") val notifyUrgentPosts: Boolean,
    @SerializedName("notify_expertise_matches_only") val notifyExpertiseMatchesOnly: Boolean,
    @SerializedName("show_phone_number") val showPhoneNumber: Boolean,
    @SerializedName("show_emergency_contact") val showEmergencyContact: Boolean,
    @SerializedName("show_medical_info") val showMedicalInfo: Boolean,
    @SerializedName("show_availability_status") val showAvailabilityStatus: Boolean,
    @SerializedName("show_bio") val showBio: Boolean,
    @SerializedName("show_location") val showLocation: Boolean,
    @SerializedName("show_resources") val showResources: Boolean,
    @SerializedName("show_expertise") val showExpertise: Boolean,
)

data class UserSettingsUpdateRequest(
    @SerializedName("notify_help_requests") val notifyHelpRequests: Boolean? = null,
    @SerializedName("notify_urgent_posts") val notifyUrgentPosts: Boolean? = null,
    @SerializedName("notify_expertise_matches_only") val notifyExpertiseMatchesOnly: Boolean? = null,
    @SerializedName("show_phone_number") val showPhoneNumber: Boolean? = null,
    @SerializedName("show_emergency_contact") val showEmergencyContact: Boolean? = null,
    @SerializedName("show_medical_info") val showMedicalInfo: Boolean? = null,
    @SerializedName("show_availability_status") val showAvailabilityStatus: Boolean? = null,
    @SerializedName("show_bio") val showBio: Boolean? = null,
    @SerializedName("show_location") val showLocation: Boolean? = null,
    @SerializedName("show_resources") val showResources: Boolean? = null,
    @SerializedName("show_expertise") val showExpertise: Boolean? = null,
)

// ── Resource ────────────────────────────────────────────────────────────────────

data class ResourceData(
    val id: Int,
    val name: String,
    val category: String,
    val quantity: Int,
    val condition: Boolean,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
)

data class ResourceCreateRequest(
    val name: String,
    val category: String,
    val quantity: Int = 1,
    val condition: Boolean = true,
)

// ── Expertise Category ──────────────────────────────────────────────────────────

data class ExpertiseCategoryData(
    val id: Int,
    val name: String,
    @SerializedName("help_request_category") val helpRequestCategory: String,
    val translations: Map<String, String> = emptyMap(),
) {
    /** Returns the localized name for [langCode], falling back to [name]. */
    fun displayName(langCode: String): String = translations[langCode] ?: name
}

// ── Expertise Field ─────────────────────────────────────────────────────────────

data class ExpertiseFieldData(
    val id: Int,
    val category: ExpertiseCategoryData,
    @SerializedName("is_approved") val isApproved: Boolean,
    @SerializedName("certification_level") val certificationLevel: String,
    @SerializedName("certification_document_url") val certificationDocumentUrl: String?,
    /**
     * Verification workflow state. One of PENDING, APPROVED, REJECTED.
     * Defaulted so older payloads still parse.
     */
    @SerializedName("verification_status") val verificationStatus: String = "PENDING",
    @SerializedName("reviewed_by_id") val reviewedById: Int? = null,
    @SerializedName("reviewed_by_name") val reviewedByName: String? = null,
    @SerializedName("reviewed_at") val reviewedAt: String? = null,
    @SerializedName("verification_note") val verificationNote: String? = null,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
)

data class ExpertiseFieldCreateRequest(
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("certification_level") val certificationLevel: String = "BEGINNER",
    @SerializedName("certification_document_url") val certificationDocumentUrl: String? = null,
)
