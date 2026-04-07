package com.bounswe2026group8.emergencyhub.offline.ui

/**
 * Represents a gathering point (e.g., shelter, hospital, meeting area)
 * loaded from the JSON file.
 */
data class GatheringPoint(

    // Display name of the location
    val name: String,

    // Latitude coordinate
    val lat: Double,

    // Longitude coordinate
    val lon: Double,

    // Short description shown to the user
    val description: String,

    // Type of location (e.g., "hospital", "shelter", "gathering")
    val type: String,

    // Optional region info (not used for filtering anymore, only informational)
    val region: String? = null
)