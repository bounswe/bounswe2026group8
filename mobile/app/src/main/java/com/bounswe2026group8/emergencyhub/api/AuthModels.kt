package com.bounswe2026group8.emergencyhub.api

import com.google.gson.annotations.SerializedName

// ── Request models ──────────────────────────────────────────────────────────────

data class RegisterRequest(
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val password: String,
    @SerializedName("confirm_password") val confirmPassword: String,
    val role: String,
    @SerializedName("neighborhood_address") val neighborhoodAddress: String? = null,
    @SerializedName("expertise_field") val expertiseField: String? = null,
    @SerializedName("hub_id") val hubId: Int? = null
)

data class Hub(
    val id: Int,
    val name: String,
    val slug: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

// ── Response models ─────────────────────────────────────────────────────────────

data class UserData(
    val id: Int,
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val role: String,
    val hub: Hub?,
    @SerializedName("neighborhood_address") val neighborhoodAddress: String?,
    @SerializedName("expertise_field") val expertiseField: String?
)

data class RegisterResponse(
    val message: String,
    val user: UserData?,
    val errors: Map<String, List<String>>?
)

data class LoginResponse(
    val message: String,
    val token: String?,
    val refresh: String?,
    val user: UserData?
)

data class LogoutResponse(
    val message: String
)

data class UpdateMeRequest(
    @SerializedName("hub_id") val hubId: Int
)

/** /me returns user data directly (no wrapper) */
typealias MeResponse = UserData
