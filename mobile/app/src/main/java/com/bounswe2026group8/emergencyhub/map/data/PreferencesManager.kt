package com.bounswe2026group8.emergencyhub.map.ui

import android.content.Context
import org.mapsforge.core.model.LatLong

class PreferencesManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)

    fun saveUserRegion(region: String) {
        prefs.edit().putString("user_region", region).apply()
    }

    fun loadUserRegion(defaultRegion: String): String {
        return prefs.getString("user_region", defaultRegion) ?: defaultRegion
    }

    fun saveSimulatedLocation(lat: Double, lon: Double) {
        prefs.edit()
            .putFloat("user_lat", lat.toFloat())
            .putFloat("user_lon", lon.toFloat())
            .apply()
    }

    fun loadSimulatedLocation(): LatLong {
        val lat = prefs.getFloat("user_lat", 41.0105f).toDouble()
        val lon = prefs.getFloat("user_lon", 28.985f).toDouble()
        return LatLong(lat, lon)
    }
}