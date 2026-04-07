package com.bounswe2026group8.emergencyhub

import com.bounswe2026group8.emergencyhub.map.data.GatheringPoint
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [GatheringPoint].
 *
 * Validates that the data class stores coordinates and type information
 * correctly — critical for the offline map feature (section 6.10 of the test plan).
 */
class GatheringPointTest {

    @Test
    fun `creates gathering point with all fields`() {
        val point = GatheringPoint(
            name = "Taksim Assembly Point",
            lat = 41.0369,
            lon = 28.9850,
            description = "Main gathering area",
            type = "gathering"
        )

        assertEquals("Taksim Assembly Point", point.name)
        assertEquals(41.0369, point.lat, 0.0001)
        assertEquals(28.9850, point.lon, 0.0001)
        assertEquals("Main gathering area", point.description)
        assertEquals("gathering", point.type)
        assertNull(point.region)
    }

    @Test
    fun `region defaults to null when not provided`() {
        val point = GatheringPoint(
            name = "Kadikoy Shelter",
            lat = 40.9903,
            lon = 29.0230,
            description = "",
            type = "shelter"
        )
        assertNull(point.region)
    }

    @Test
    fun `region can be set explicitly`() {
        val point = GatheringPoint(
            name = "Uskudar Hospital",
            lat = 41.0256,
            lon = 29.0156,
            description = "Emergency hospital",
            type = "hospital",
            region = "Uskudar"
        )
        assertEquals("Uskudar", point.region)
    }

    @Test
    fun `valid type values are hospital`() {
        val point = GatheringPoint("Hospital", 41.0, 29.0, "", "hospital")
        assertEquals("hospital", point.type)
    }

    @Test
    fun `valid type values are shelter`() {
        val point = GatheringPoint("Shelter", 41.0, 29.0, "", "shelter")
        assertEquals("shelter", point.type)
    }

    @Test
    fun `valid type values are fire station`() {
        val point = GatheringPoint("Fire Station", 41.0, 29.0, "", "fire_station")
        assertEquals("fire_station", point.type)
    }

    @Test
    fun `valid type values are police`() {
        val point = GatheringPoint("Police", 41.0, 29.0, "", "police")
        assertEquals("police", point.type)
    }

    @Test
    fun `two points with same coordinates are equal`() {
        val a = GatheringPoint("Point A", 41.0, 29.0, "desc", "shelter")
        val b = GatheringPoint("Point A", 41.0, 29.0, "desc", "shelter")
        assertEquals(a, b)
    }

    @Test
    fun `two points with different coordinates are not equal`() {
        val a = GatheringPoint("Point", 41.0, 29.0, "", "shelter")
        val b = GatheringPoint("Point", 41.1, 29.0, "", "shelter")
        assertNotEquals(a, b)
    }

    @Test
    fun `radius constant is 5km`() {
        // Ensure the search radius used for offline map hasn't been accidentally changed
        assertEquals(5.0, com.bounswe2026group8.emergencyhub.map.data.GatheringPointCache.RADIUS_KM, 0.0)
    }
}
