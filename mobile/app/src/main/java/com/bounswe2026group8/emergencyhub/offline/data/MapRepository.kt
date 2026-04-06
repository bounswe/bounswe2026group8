package com.bounswe2026group8.emergencyhub.offline.ui

import android.content.Context
import com.bounswe2026group8.emergencyhub.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.mapsforge.core.model.LatLong
import java.text.Normalizer
import java.util.Locale
import kotlin.math.*

data class WorldRegion(
    val name: String,
    val countries: List<String>
)

class MapRepository(private val context: Context) {

    private val worldRegions: List<WorldRegion> by lazy {
        val json = context.assets.open("world.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<WorldRegion>>() {}.type
        Gson().fromJson(json, type)
    }

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

    private fun normalizeSlug(value: String): String {
        val lowered = value.trim().lowercase(Locale.US)

        val normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")

        return normalized
            .replace("&", "and")
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
    }

    fun isUnitedStates(countryName: String): Boolean {
        val slug = normalizeSlug(countryName)
        return slug == "united-states" ||
                slug == "united-states-of-america" ||
                slug == "usa" ||
                slug == "us"
    }

    fun normalizeUsStateSlug(stateNameOrCode: String): String {
        val trimmed = stateNameOrCode.trim()
        val upper = trimmed.uppercase(Locale.US)
        return usStateCodeToSlug[upper] ?: normalizeSlug(trimmed)
    }

    fun getRegionForCountry(countryName: String): String {
        val slug = normalizeSlug(countryName)

        return worldRegions.firstOrNull { region ->
            region.countries.any { it.equals(slug, ignoreCase = true) }
        }?.name ?: "europe"
    }

    fun countryExistsInWorldJson(countryName: String): Boolean {
        val slug = normalizeSlug(countryName)

        return worldRegions.any { region ->
            region.countries.any { it.equals(slug, ignoreCase = true) }
        }
    }

    fun buildCountryMapUrl(countryName: String): String {
        val region = getRegionForCountry(countryName)
        val slug = normalizeSlug(countryName)
        return "https://download.mapsforge.org/maps/v5/$region/$slug.map"
    }

    fun buildUsStateMapUrl(stateNameOrCode: String): String {
        val stateSlug = normalizeUsStateSlug(stateNameOrCode)
        return "https://download.mapsforge.org/maps/v5/north-america/us/$stateSlug.map"
    }

    fun getCountryMapFileName(countryName: String): String {
        return "${normalizeSlug(countryName)}.offline"
    }

    fun getUsStateMapFileName(stateNameOrCode: String): String {
        return "${normalizeUsStateSlug(stateNameOrCode)}.offline"
    }

    fun getAllGatheringPoints(): List<GatheringPoint> {
        val inputStream = context.resources.openRawResource(R.raw.gathering_points)
        val json = inputStream.bufferedReader().use { it.readText() }
        return Gson().fromJson(json, Array<GatheringPoint>::class.java).toList()
    }

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

    fun getNearbyGatheringPoints(
        user: LatLong,
        radiusKm: Double
    ): List<GatheringPoint> {
        return getAllGatheringPoints().filter { point ->
            val pointLocation = LatLong(point.lat, point.lon)
            distanceKm(user, pointLocation) <= radiusKm
        }
    }

    fun findNearestPoint(
        user: LatLong,
        points: List<GatheringPoint>
    ): GatheringPoint? {
        return points.minByOrNull { point ->
            distanceKm(user, LatLong(point.lat, point.lon))
        }
    }
}