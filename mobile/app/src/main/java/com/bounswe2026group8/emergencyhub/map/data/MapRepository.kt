package com.bounswe2026group8.emergencyhub.map.data

import android.content.Context
import com.bounswe2026group8.emergencyhub.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.mapsforge.core.model.LatLong
import java.text.Normalizer
import java.util.Locale
import kotlin.math.*

/**
 * Represents a region (like "europe") and the countries inside it.
 * Loaded from world.json.
 */
data class WorldRegion(
    val name: String,
    val countries: List<String>
)

/**
 * Handles:
 * - map URL generation
 * - country/state normalization
 * - gathering point loading & filtering
 */
class MapRepository(private val context: Context) {

    /**
     * Lazy-loaded list of world regions from assets/world.json.
     * Used to map country -> region (e.g., Turkey -> europe).
     */
    private val worldRegions: List<WorldRegion> by lazy {
        val json = context.assets.open("world.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<WorldRegion>>() {}.type
        Gson().fromJson(json, type)
    }

    /**
     * Maps US state codes (CA, NY, etc.) to Mapsforge-compatible slugs.
     */
    private val usStateCodeToSlug = mapOf(
        "AL" to "alabama",
        "AK" to "alaska",
        "AZ" to "arizona",
        "AR" to "arkansas",
        "CA" to "california",
        "CO" to "colorado",
        "CT" to "connecticut",
        "DE" to "delaware",
        "FL" to "florida",
        "GA" to "georgia",
        "HI" to "hawaii",
        "ID" to "idaho",
        "IL" to "illinois",
        "IN" to "indiana",
        "IA" to "iowa",
        "KS" to "kansas",
        "KY" to "kentucky",
        "LA" to "louisiana",
        "ME" to "maine",
        "MD" to "maryland",
        "MA" to "massachusetts",
        "MI" to "michigan",
        "MN" to "minnesota",
        "MS" to "mississippi",
        "MO" to "missouri",
        "MT" to "montana",
        "NE" to "nebraska",
        "NV" to "nevada",
        "NH" to "new-hampshire",
        "NJ" to "new-jersey",
        "NM" to "new-mexico",
        "NY" to "new-york",
        "NC" to "north-carolina",
        "ND" to "north-dakota",
        "OH" to "ohio",
        "OK" to "oklahoma",
        "OR" to "oregon",
        "PA" to "pennsylvania",
        "RI" to "rhode-island",
        "SC" to "south-carolina",
        "SD" to "south-dakota",
        "TN" to "tennessee",
        "TX" to "texas",
        "UT" to "utah",
        "VT" to "vermont",
        "VA" to "virginia",
        "WA" to "washington",
        "WV" to "west-virginia",
        "WI" to "wisconsin",
        "WY" to "wyoming",
        "DC" to "district-of-columbia"
    )

    /**
     * Converts names like "United States" or "New York"
     * into URL-safe slugs like "united-states", "new-york".
     */
    private fun normalizeSlug(value: String): String {
        val lowered = value.trim().lowercase(Locale.US)

        val normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "") // remove accents

        return normalized
            .replace("&", "and")
            .replace("[^a-z0-9]+".toRegex(), "-") // replace spaces/symbols
            .trim('-')
    }

    /**
     * Checks if a country name refers to the USA.
     * Handles different formats (USA, US, United States, etc.)
     */
    fun isUnitedStates(countryName: String): Boolean {
        val slug = normalizeSlug(countryName)
        return slug == "united-states" ||
                slug == "united-states-of-america" ||
                slug == "usa" ||
                slug == "us"
    }

    /**
     * Converts a US state (name or code) into Mapsforge slug.
     * Example: "CA" -> "california", "New York" -> "new-york"
     */
    fun normalizeUsStateSlug(stateNameOrCode: String): String {
        val trimmed = stateNameOrCode.trim()
        val upper = trimmed.uppercase(Locale.US)
        return usStateCodeToSlug[upper] ?: normalizeSlug(trimmed)
    }

    /**
     * Finds which region (e.g., europe) a country belongs to.
     * Uses world.json.
     */
    fun getRegionForCountry(countryName: String): String {
        val slug = normalizeSlug(countryName)

        return worldRegions.firstOrNull { region ->
            region.countries.any { it.equals(slug, ignoreCase = true) }
        }?.name ?: "europe" // fallback
    }

    /**
     * Checks if a country exists in world.json.
     */
    fun countryExistsInWorldJson(countryName: String): Boolean {
        val slug = normalizeSlug(countryName)

        return worldRegions.any { region ->
            region.countries.any { it.equals(slug, ignoreCase = true) }
        }
    }

    /**
     * Builds Mapsforge URL for a country.
     * Example: europe/turkey.map
     */
    fun buildCountryMapUrl(countryName: String): String {
        val region = getRegionForCountry(countryName)
        val slug = normalizeSlug(countryName)
        return "https://download.mapsforge.org/maps/v5/$region/$slug.map"
    }

    /**
     * Builds Mapsforge URL for US state maps.
     * Example: north-america/us/california.map
     */
    fun buildUsStateMapUrl(stateNameOrCode: String): String {
        val stateSlug = normalizeUsStateSlug(stateNameOrCode)
        return "https://download.mapsforge.org/maps/v5/north-america/us/$stateSlug.map"
    }

    /**
     * File name for country map (used in storage).
     */
    fun getCountryMapFileName(countryName: String): String {
        return "${normalizeSlug(countryName)}.map"
    }

    /**
     * File name for US state map.
     */
    fun getUsStateMapFileName(stateNameOrCode: String): String {
        return "${normalizeUsStateSlug(stateNameOrCode)}.map"
    }

    /**
     * Loads all gathering points from JSON (no filtering here).
     */
    fun getAllGatheringPoints(): List<GatheringPoint> {
        val inputStream = context.resources.openRawResource(R.raw.gathering_points)
        val json = inputStream.bufferedReader().use { it.readText() }
        return Gson().fromJson(json, Array<GatheringPoint>::class.java).toList()
    }

    /**
     * Calculates distance between two coordinates (in kilometers)
     * using Haversine formula.
     */
    fun distanceKm(a: LatLong, b: LatLong): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)

        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)

        val hav =
            sin(dLat / 2).pow(2) +
                    cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(hav), sqrt(1 - hav))
        return earthRadiusKm * c
    }

    /**
     * Returns only points within a given radius from the user.
     * This replaces old region-based filtering.
     */
    fun getNearbyGatheringPoints(
        user: LatLong,
        radiusKm: Double
    ): List<GatheringPoint> {
        return getAllGatheringPoints().filter { point ->
            val pointLocation = LatLong(point.lat, point.lon)
            distanceKm(user, pointLocation) <= radiusKm
        }
    }

    /**
     * Finds the closest point among given points.
     */
    fun findNearestPoint(
        user: LatLong,
        points: List<GatheringPoint>
    ): GatheringPoint? {
        return points.minByOrNull { point ->
            distanceKm(user, LatLong(point.lat, point.lon))
        }
    }
}