package com.bounswe2026group8.emergencyhub.offline.rendering

import android.content.Context
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import com.bounswe2026group8.emergencyhub.R
import org.mapsforge.map.android.view.MapView
import java.io.File
import java.util.Locale
import com.bounswe2026group8.emergencyhub.offline.data.PreferencesManager
import com.bounswe2026group8.emergencyhub.offline.data.MapRepository
import com.bounswe2026group8.emergencyhub.offline.data.GatheringPoint

/**
 * Controls what is shown on the map screen.
 *
 * Responsibilities:
 * - load the correct .map file
 * - center the map on user location
 * - find nearby gathering points
 * - update UI text
 * - add markers
 */
class MapScreenController(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val mapRepository: MapRepository,
    private val mapRenderer: MapRenderer
) {

    /**
     * Main function called when map needs to be displayed/refreshed.
     */
    fun updateMap(
        mapView: MapView,
        infoText: TextView
    ) {
        // Clear previous layers (map + markers)
        mapView.layerManager.layers.clear()

        // Load saved map file name (e.g., "california.map", "turkey.map")
        val mapFileName = preferencesManager.loadMapFileName("turkey.offline")

        // Get app-specific storage directory
        val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val mapFile = File(appDir, mapFileName)

        // If file does not exist, we cannot render map
        if (!mapFile.exists()) {
            Toast.makeText(
                context,
                "Map file not found: $mapFileName",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Try to load map safely
        try {
            mapRenderer.loadMap(mapView, mapFile)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Map load failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Load user's last known location
        val userLocation = preferencesManager.loadUserLocation()

        // Center map on user
        mapView.setCenter(userLocation)
        mapView.setZoomLevel(12.toByte())

        // Get nearby points based on distance (not region anymore)
        val nearbyPoints = mapRepository.getNearbyGatheringPoints(
            user = userLocation,
            radiusKm = 20.0
        )

        // Find closest point
        val nearestPoint = mapRepository.findNearestPoint(userLocation, nearbyPoints)

        // Update info text (top label)
        infoText.text = if (nearestPoint != null) {

            val distanceKm = mapRepository.distanceKm(
                userLocation,
                org.mapsforge.core.model.LatLong(nearestPoint.lat, nearestPoint.lon)
            )

            context.getString(
                R.string.nearest_format,
                "${nearestPoint.name} (${String.format(Locale.US, "%.2f", distanceKm)} km)"
            )
        } else {
            context.getString(
                R.string.nearest_format,
                "No nearby point"
            )
        }

        // Add markers to map
        mapRenderer.addMarkers(mapView, nearbyPoints, userLocation, nearestPoint)

        // Refresh map view
        mapView.invalidate()
    }
}