package com.bounswe2026group8.emergencyhub.map.ui

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import org.mapsforge.map.android.view.MapView
import java.io.File
import com.bounswe2026group8.emergencyhub.R

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

        val region = preferencesManager.loadUserRegion(defaultRegion)
        val mapFileName = mapRepository.getMapFileNameForRegion(region)

        val appDir = context.getExternalFilesDir(null)
        val mapFile = File(appDir, mapFileName)

        if (mapFile.exists()) {
            mapRenderer.loadMap(mapView, mapFile)

            val gatheringPoints = mapRepository.getGatheringPoints(region)
            val userLocation = preferencesManager.loadSimulatedLocation()
            val nearestPoint = mapRepository.findNearestPoint(userLocation, gatheringPoints)

            if (nearestPoint != null) {
                infoText.text = context.getString(
                    R.string.nearest_format,
                    "${nearestPoint.name} ($region)"
                )
            } else {
                infoText.text = context.getString(
                    R.string.nearest_format,
                    "No point ($region)"
                )
            }

            mapRenderer.addMarkers(mapView, gatheringPoints, userLocation, nearestPoint)
            mapView.invalidate()
        } else {
            infoText.text = context.getString(
                R.string.nearest_format,
                "No point ($region)"
            )

            Toast.makeText(
                context,
                "Map file not found for region",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
