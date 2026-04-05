package com.bounswe2026group8.emergencyhub.api

import com.google.gson.annotations.SerializedName

/**
 * Data classes for the Help Offers API.
 * Reuses [HelpRequestAuthor] for the nested author object since
 * the backend uses the same UserSerializer for both domains.
 */

/** Single item from GET /help-offers/ and POST /help-offers/ responses. */
data class HelpOfferItem(
    val id: Int,
    val hub: Int?,
    @SerializedName("hub_name") val hubName: String?,
    val category: String,
    val author: HelpRequestAuthor,
    @SerializedName("skill_or_resource") val skillOrResource: String,
    val description: String,
    val availability: String,
    @SerializedName("created_at") val createdAt: String
)

/** Request body for POST /help-offers/. */
data class CreateHelpOffer(
    val hub: Int? = null,
    val category: String,
    @SerializedName("skill_or_resource") val skillOrResource: String,
    val description: String,
    val availability: String
)
