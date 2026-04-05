package com.bounswe2026group8.emergencyhub.map.ui

import android.content.Context
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import com.bounswe2026group8.emergencyhub.R
import org.mapsforge.map.android.view.MapView
import java.io.File

class MapScreenController(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val mapRepository: MapRepository,
    private val mapRenderer: MapRenderer
) {

    fun updateMap(
        mapView: MapView,
        infoText: TextView,
        defaultRegion: String
    ) {
        mapView.layerManager.layers.clear()

        val mapFileName = preferencesManager.loadMapFileName("turkey.map")
        val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val mapFile = File(appDir, mapFileName)

        if (!mapFile.exists()) {
            infoText.text = "Map file missing: $mapFileName"

            Toast.makeText(
                context,
                "Map file not found: $mapFileName",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            mapRenderer.loadMap(mapView, mapFile)
        } catch (e: Exception) {
            infoText.text = "Map load failed: ${e.message}"

            Toast.makeText(
                context,
                "Map load failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val userLocation = preferencesManager.loadUserLocation()
        mapView.setCenter(userLocation)
        mapView.setZoomLevel(12.toByte())

        val nearbyPoints = mapRepository.getNearbyGatheringPoints(
            user = userLocation,
            radiusKm = 20.0
        )

        val nearestPoint = mapRepository.findNearestPoint(userLocation, nearbyPoints)

        infoText.text = if (nearestPoint != null) {
            val distanceKm = mapRepository.distanceKm(
                userLocation,
                org.mapsforge.core.model.LatLong(nearestPoint.lat, nearestPoint.lon)
            )
            context.getString(
                R.string.nearest_format,
                "${nearestPoint.name} (${String.format("%.2f", distanceKm)} km)"
            )
        } else {
            context.getString(
                R.string.nearest_format,
                "No nearby point"
            )
        }

        mapRenderer.addMarkers(mapView, nearbyPoints, userLocation, nearestPoint)
        mapView.invalidate()
    }
}