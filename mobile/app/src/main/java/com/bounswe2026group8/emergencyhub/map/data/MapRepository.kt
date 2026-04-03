package com.bounswe2026group8.emergencyhub.map.ui

import android.content.Context
import com.google.gson.Gson
import org.mapsforge.core.model.LatLong
import com.bounswe2026group8.emergencyhub.R

class MapRepository(private val context: Context) {

    fun getGatheringPoints(region: String): List<GatheringPoint> {
        val inputStream = context.resources.openRawResource(R.raw.gathering_points)
        val json = inputStream.bufferedReader().use { it.readText() }

        val gson = Gson()
        val array = gson.fromJson(json, Array<GatheringPoint>::class.java)

        return array.filter {
            it.region.equals(region, ignoreCase = true)
        }
    }

    fun distance(a: LatLong, b: LatLong): Double {
        val dx = a.latitude - b.latitude
        val dy = a.longitude - b.longitude
        return Math.sqrt(dx * dx + dy * dy)
    }

    fun findNearestPoint(
        user: LatLong,
        points: List<GatheringPoint>
    ): GatheringPoint? {
        return points.minByOrNull {
            distance(user, LatLong(it.lat, it.lon))
        }
    }

    fun getRegionFromLocation(location: LatLong): String {
        return if (location.latitude > 40.5) "istanbul" else "ankara"
    }

    fun getMapFileNameForRegion(region: String): String {
        return when (region.lowercase()) {
            "istanbul" -> "Istanbul.map"
            "ankara" -> "Ankara.map"
            else -> "Istanbul.map"
        }
    }
}