package com.bounswe2026group8.emergencyhub.map.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Fetches emergency assembly points from the Overpass API and caches them locally.
 *
 * Online:  queries Overpass for "emergency"="assembly_point" and "amenity"="shelter"
 *          within RADIUS_KM of the user, saves result to SharedPreferences.
 * Offline: returns the last cached result.
 * No data: returns empty list (caller shows "No nearby point").
 *
 * Cache is considered stale after 24 hours OR if the user moved more than 2km
 * from the cached center.
 */
class GatheringPointCache(private val context: Context) {

    private val prefs = context.getSharedPreferences("gathering_points_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GatheringPointCache"
        const val RADIUS_KM = 5.0
        private const val CACHE_VALID_MS = 24 * 60 * 60 * 1000L
        private const val CACHE_MOVE_THRESHOLD_KM = 2.0
    }

    /**
     * Returns gathering points near (lat, lon).
     * Runs on IO dispatcher — safe to call from a coroutine.
     */
    suspend fun getPoints(lat: Double, lon: Double): List<GatheringPoint> =
        withContext(Dispatchers.IO) {
            if (isCacheValid(lat, lon)) {
                return@withContext loadFromCache() ?: emptyList()
            }
            try {
                val points = fetchFromOverpass(lat, lon)
                if (points.isNotEmpty()) saveToCache(points, lat, lon)
                points
            } catch (e: Exception) {
                Log.w(TAG, "Overpass fetch failed: ${e.message}")
                // Only use cache if it's near the requested location — never
                // show stale points from a completely different area.
                if (isCacheNearby(lat, lon)) loadFromCache() ?: emptyList()
                else emptyList()
            }
        }

    /** Returns true if the cached center is within the search radius of (lat, lon). */
    private fun isCacheNearby(lat: Double, lon: Double): Boolean {
        val cachedLat = prefs.getFloat("lat", Float.NaN).toDouble()
        val cachedLon = prefs.getFloat("lon", Float.NaN).toDouble()
        if (cachedLat.isNaN()) return false
        return haversineKm(lat, lon, cachedLat, cachedLon) <= RADIUS_KM
    }

    private fun isCacheValid(lat: Double, lon: Double): Boolean {
        val json = prefs.getString("points", null) ?: return false
        val cachedLat = prefs.getFloat("lat", Float.NaN).toDouble()
        val cachedLon = prefs.getFloat("lon", Float.NaN).toDouble()
        val cachedTime = prefs.getLong("time", 0L)
        if (cachedLat.isNaN()) return false
        val ageOk = System.currentTimeMillis() - cachedTime < CACHE_VALID_MS
        val distOk = haversineKm(lat, lon, cachedLat, cachedLon) <= CACHE_MOVE_THRESHOLD_KM
        return ageOk && distOk
    }

    private fun loadFromCache(): List<GatheringPoint>? {
        val json = prefs.getString("points", null) ?: return null
        return try {
            val type = object : TypeToken<List<GatheringPoint>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToCache(points: List<GatheringPoint>, lat: Double, lon: Double) {
        prefs.edit()
            .putString("points", gson.toJson(points))
            .putFloat("lat", lat.toFloat())
            .putFloat("lon", lon.toFloat())
            .putLong("time", System.currentTimeMillis())
            .apply()
    }

    private fun fetchFromOverpass(lat: Double, lon: Double): List<GatheringPoint> {
        val radiusM = (RADIUS_KM * 1000).toInt()
        val query = """
            [out:json][timeout:25];
            (
              node["emergency"="assembly_point"](around:$radiusM,$lat,$lon);
              node["amenity"="shelter"](around:$radiusM,$lat,$lon);
              node["amenity"="hospital"](around:$radiusM,$lat,$lon);
              node["amenity"="clinic"](around:$radiusM,$lat,$lon);
              node["amenity"="fire_station"](around:$radiusM,$lat,$lon);
              node["amenity"="police"](around:$radiusM,$lat,$lon);
              way["amenity"="hospital"](around:$radiusM,$lat,$lon);
              way["amenity"="fire_station"](around:$radiusM,$lat,$lon);
            );
            out center;
        """.trimIndent()

        val body = "data=${java.net.URLEncoder.encode(query, "UTF-8")}"
            .toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder()
            .url("https://overpass-api.de/api/interpreter")
            .post(body)
            .header("User-Agent", context.packageName)
            .build()

        Log.d(TAG, "Fetching from Overpass: lat=$lat, lon=$lon, radius=${radiusM}m")
        val response = client.newCall(request).execute()
        Log.d(TAG, "Overpass response code: ${response.code}")
        if (!response.isSuccessful) {
            Log.e(TAG, "Overpass HTTP error: ${response.code} ${response.message}")
            return emptyList()
        }
        val responseBody = response.body?.string() ?: return emptyList()
        Log.d(TAG, "Overpass response length: ${responseBody.length} chars")
        val points = parseResponse(responseBody)
        Log.d(TAG, "Parsed ${points.size} gathering points")
        return points
    }

    private fun parseResponse(json: String): List<GatheringPoint> {
        return try {
            val elements = JsonParser.parseString(json)
                .asJsonObject
                .getAsJsonArray("elements")

            elements.mapNotNull { el ->
                try {
                    val obj = el.asJsonObject
                    // nodes have lat/lon directly; ways have a "center" object
                    val lat: Double
                    val lon: Double
                    if (obj.has("lat")) {
                        lat = obj.get("lat").asDouble
                        lon = obj.get("lon").asDouble
                    } else {
                        val center = obj.getAsJsonObject("center")
                        lat = center.get("lat").asDouble
                        lon = center.get("lon").asDouble
                    }
                    val tags = obj.getAsJsonObject("tags")
                    val name = tags?.get("name")?.asString
                        ?: tags?.get("name:en")?.asString
                    val amenity = tags?.get("amenity")?.asString
                    val emergency = tags?.get("emergency")?.asString
                    val type = when {
                        emergency == "assembly_point" -> "gathering"
                        amenity == "hospital" || amenity == "clinic" -> "hospital"
                        amenity == "fire_station" -> "fire_station"
                        amenity == "police" -> "police"
                        else -> "shelter"
                    }
                    val displayName = name ?: when (type) {
                        "hospital" -> "Hospital"
                        "fire_station" -> "Fire Station"
                        "police" -> "Police Station"
                        "gathering" -> "Assembly Point"
                        else -> "Shelter"
                    }
                    GatheringPoint(name = displayName, lat = lat, lon = lon, description = "", type = type)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Overpass response: ${e.message}")
            emptyList()
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
