package com.bounswe2026group8.emergencyhub.offline.data

import android.content.Context
import org.mapsforge.core.model.LatLong

/**
 * Handles saving/loading small persistent data using SharedPreferences.
 *
 * We use this to remember:
 * - last known user location
 * - detected country
 * - selected map file (e.g., california.map)
 */
class PreferencesManager(private val context: Context) {

    // SharedPreferences instance for this feature
    private val prefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)

    /**
     * Save region (e.g., "europe", "north-america")
     * Not heavily used anymore after switching to distance-based logic.
     */
    fun saveUserRegion(region: String) {
        prefs.edit().putString("user_region", region).apply()
    }

    /**
     * Load saved region, fallback to default if not found.
     */
    fun loadUserRegion(defaultRegion: String): String {
        return prefs.getString("user_region", defaultRegion) ?: defaultRegion
    }

    /**
     * Save user's last known location.
     */
    fun saveUserLocation(lat: Double, lon: Double) {
        prefs.edit()
            .putFloat("user_lat", lat.toFloat())
            .putFloat("user_lon", lon.toFloat())
            .apply()
    }

    /**
     * Load last saved location.
     * Defaults to Istanbul if nothing is stored.
     */
    fun loadUserLocation(
        defaultLat: Double = 41.0105,
        defaultLon: Double = 28.985
    ): LatLong {
        val lat = prefs.getFloat("user_lat", defaultLat.toFloat()).toDouble()
        val lon = prefs.getFloat("user_lon", defaultLon.toFloat()).toDouble()
        return LatLong(lat, lon)
    }

    /**
     * Save detected country (e.g., "Turkey", "United States")
     */
    fun saveUserCountry(countryName: String) {
        prefs.edit().putString("user_country", countryName).apply()
    }

    /**
     * Load last detected country.
     */
    fun loadUserCountry(defaultCountry: String = "Turkey"): String {
        return prefs.getString("user_country", defaultCountry) ?: defaultCountry
    }

    /**
     * Save the exact map file name we downloaded.
     * Example: "turkey.map", "california.map"
     *
     * This avoids recomputing file names later.
     */
    fun saveMapFileName(fileName: String) {
        prefs.edit().putString("user_map_file_name", fileName).apply()
    }

    /**
     * Load saved map file name.
     * Used directly when rendering the map.
     */
    fun loadMapFileName(defaultFileName: String = "turkey.offline"): String {
        return prefs.getString("user_map_file_name", defaultFileName) ?: defaultFileName
    }
}