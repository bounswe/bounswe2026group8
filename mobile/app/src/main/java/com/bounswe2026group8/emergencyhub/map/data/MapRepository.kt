package com.bounswe2026group8.emergencyhub.map.data

import kotlin.math.*

/**
 * Distance and nearest-point utilities for gathering point logic.
 */
class MapRepository {

    /**
     * Calculates distance between two coordinates in kilometers (Haversine formula).
     */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    /**
     * Finds the closest point in the list to (lat, lon).
     */
    fun findNearestPoint(
        lat: Double,
        lon: Double,
        points: List<GatheringPoint>
    ): GatheringPoint? {
        return points.minByOrNull { point ->
            distanceKm(lat, lon, point.lat, point.lon)
        }
    }
}
