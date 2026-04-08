package com.bounswe2026group8.emergencyhub.map.data

/**
 * Represents a gathering point (e.g., shelter, hospital, meeting area)
 * loaded from the Overpass API or local cache.
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

    // Type of location (e.g., "hospital", "shelter", "gathering", "fire_station", "police")
    val type: String,

    // Optional region info (informational only)
    val region: String? = null
)
