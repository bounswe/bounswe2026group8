package com.bounswe2026group8.emergencyhub.offline.ui

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

    fun saveUserLocation(lat: Double, lon: Double) {
        prefs.edit()
            .putFloat("user_lat", lat.toFloat())
            .putFloat("user_lon", lon.toFloat())
            .apply()
    }

    fun loadUserLocation(defaultLat: Double = 41.0105, defaultLon: Double = 28.985): LatLong {
        val lat = prefs.getFloat("user_lat", defaultLat.toFloat()).toDouble()
        val lon = prefs.getFloat("user_lon", defaultLon.toFloat()).toDouble()
        return LatLong(lat, lon)
    }

    fun saveUserCountry(countryName: String) {
        prefs.edit().putString("user_country", countryName).apply()
    }

    fun loadUserCountry(defaultCountry: String = "Turkey"): String {
        return prefs.getString("user_country", defaultCountry) ?: defaultCountry
    }

    fun saveMapFileName(fileName: String) {
        prefs.edit().putString("user_map_file_name", fileName).apply()
    }

    fun loadMapFileName(defaultFileName: String = "turkey.offline"): String {
        return prefs.getString("user_map_file_name", defaultFileName) ?: defaultFileName
    }
}