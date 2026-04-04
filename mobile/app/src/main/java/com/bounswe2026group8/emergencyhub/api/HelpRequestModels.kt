package com.bounswe2026group8.emergencyhub.api

import com.google.gson.annotations.SerializedName

/**
 * Data classes for the Help Requests API responses.
 *
 * The backend's UserSerializer includes a nested hub object that the
 * existing UserData class does not have, so we define separate models
 * for this domain.
 */

/** Nested hub object returned inside the author field. */
data class HubData(
    val id: Int,
    val name: String,
    val slug: String
)

/** Author object as returned by the help-requests endpoints. */
data class HelpRequestAuthor(
    val id: Int,
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val role: String,
    val hub: HubData?,
    @SerializedName("neighborhood_address") val neighborhoodAddress: String?,
    @SerializedName("expertise_field") val expertiseField: String?
)

/** Single item from GET /help-requests/ list response. */
data class HelpRequestItem(
    val id: Int,
    val hub: Int?,
    @SerializedName("hub_name") val hubName: String?,
    val category: String,
    val urgency: String,
    val author: HelpRequestAuthor,
    val title: String,
    val status: String,
    @SerializedName("comment_count") val commentCount: Int,
    @SerializedName("created_at") val createdAt: String
)

/** Request body for POST /help-requests/. */
data class CreateHelpRequest(
    val hub: Int? = null,
    val category: String,
    val urgency: String,
    val title: String,
    val description: String,
    @SerializedName("image_urls") val imageUrls: List<String> = emptyList(),
    val latitude: String? = null,
    val longitude: String? = null,
    @SerializedName("location_text") val locationText: String? = null
)

/** Detail response from POST /help-requests/ and GET /help-requests/{id}/. */
data class HelpRequestDetail(
    val id: Int,
    val hub: Int?,
    @SerializedName("hub_name") val hubName: String?,
    val category: String,
    val urgency: String,
    val author: HelpRequestAuthor,
    val title: String,
    val description: String,
    @SerializedName("image_urls") val imageUrls: List<String> = emptyList(),
    val latitude: String?,
    val longitude: String?,
    @SerializedName("location_text") val locationText: String?,
    val status: String,
    @SerializedName("comment_count") val commentCount: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

/** Single comment from GET /help-requests/{id}/comments/. */
data class HelpRequestComment(
    val id: Int,
    val request: Int,
    val author: HelpRequestAuthor,
    val content: String,
    @SerializedName("created_at") val createdAt: String
)
