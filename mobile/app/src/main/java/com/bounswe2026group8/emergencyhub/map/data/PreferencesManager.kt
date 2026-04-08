package com.bounswe2026group8.emergencyhub.map.data

import android.content.Context

/**
 * Handles saving/loading small persistent data using SharedPreferences.
 *
 * We use this to remember:
 * - last known user location
 * - detected country
 */
class PreferencesManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)

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
     * Load last saved location as (latitude, longitude).
     * Defaults to Istanbul if nothing is stored.
     */
    fun loadUserLocation(
        defaultLat: Double = 41.0105,
        defaultLon: Double = 28.985
    ): Pair<Double, Double> {
        val lat = prefs.getFloat("user_lat", defaultLat.toFloat()).toDouble()
        val lon = prefs.getFloat("user_lon", defaultLon.toFloat()).toDouble()
        return Pair(lat, lon)
    }

    /**
     * Save detected country (e.g., "Turkey", "United States")
     */
    fun saveUserCountry(countryName: String) {
        prefs.edit().putString("user_country", countryName).apply()
    }
}
