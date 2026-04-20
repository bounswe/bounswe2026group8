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
)

// ── Expertise Field ─────────────────────────────────────────────────────────────

data class ExpertiseFieldData(
    val id: Int,
    val category: ExpertiseCategoryData,
    @SerializedName("is_approved") val isApproved: Boolean,
    @SerializedName("certification_level") val certificationLevel: String,
    @SerializedName("certification_document_url") val certificationDocumentUrl: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
)

data class ExpertiseFieldCreateRequest(
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("certification_level") val certificationLevel: String = "BEGINNER",
    @SerializedName("certification_document_url") val certificationDocumentUrl: String? = null,
)
