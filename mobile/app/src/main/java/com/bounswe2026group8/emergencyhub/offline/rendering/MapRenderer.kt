package com.bounswe2026group8.emergencyhub.offline.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import java.io.File
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.offline.data.GatheringPoint

/**
 * Responsible for:
 * - loading the .map file into MapView
 * - rendering markers (user + gathering points)
 */
class MapRenderer(private val context: Context) {

    /**
     * Loads a Mapsforge .map file into the MapView.
     */
    fun loadMap(mapView: MapView, mapFile: File) {

        // Safety check: file must exist and not be empty
        if (!mapFile.exists() || mapFile.length() == 0L) {
            throw IllegalArgumentException("Map file missing or empty: ${mapFile.absolutePath}")
        }

        // Create tile cache (improves map performance)
        val tileCache = AndroidUtil.createTileCache(
            context,
            "mapcache",
            mapView.model.displayModel.tileSize,
            1f,
            mapView.model.frameBufferModel.overdrawFactor
        )

        // Read map data from file
        val mapDataStore = MapFile(mapFile)

        // Create renderer layer that connects map data to the view
        val tileRendererLayer = TileRendererLayer(
            tileCache,
            mapDataStore,
            mapView.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE
        ).apply {
            // Use default Mapsforge styling
            setXmlRenderTheme(InternalRenderTheme.DEFAULT)
        }

        // Add layer to map (this actually displays the map)
        mapView.layerManager.layers.add(tileRendererLayer)
    }

    /**
     * Adds:
     * - user location marker
     * - nearby gathering point markers
     * - highlights nearest point
     */
    fun addMarkers(
        mapView: MapView,
        gatheringPoints: List<GatheringPoint>,
        userLocation: LatLong,
        nearestPoint: GatheringPoint?
    ) {

        // Load marker images from resources
        val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.marker)
        val userBitmapOriginal = BitmapFactory.decodeResource(context.resources, R.drawable.user_marker)

        // Resize user marker icon
        val userBitmap = android.graphics.Bitmap.createScaledBitmap(
            userBitmapOriginal,
            80, 80,
            true
        )

        // -------------------------
        // USER MARKER
        // -------------------------
        val userMarker = Marker(
            userLocation,
            org.mapsforge.map.android.graphics.AndroidBitmap(userBitmap),
            0,
            -userBitmap.height / 2 // anchor bottom-center
        )

        mapView.layerManager.layers.add(userMarker)

        // -------------------------
        // GATHERING POINT MARKERS
        // -------------------------
        for (point in gatheringPoints) {

            // Make nearest point larger
            val isNearest = point == nearestPoint
            val size = if (isNearest) 120 else 80

            val bitmap = android.graphics.Bitmap.createScaledBitmap(
                originalBitmap,
                size, size,
                true
            )

            // Custom marker with click behavior
            val marker = object : Marker(
                LatLong(point.lat, point.lon),
                org.mapsforge.map.android.graphics.AndroidBitmap(bitmap),
                0,
                -bitmap.height / 2
            ) {

                /**
                 * Called when user taps on marker.
                 * Shows point name as a toast.
                 */
                override fun onTap(
                    tapLatLong: LatLong?,
                    layerXY: org.mapsforge.core.model.Point?,
                    tapXY: org.mapsforge.core.model.Point?
                ): Boolean {
                    if (contains(layerXY, tapXY)) {
                        Toast.makeText(
                            context,
                            point.name,
                            Toast.LENGTH_SHORT
                        ).show()
                        return true
                    }
                    return false
                }
            }

            mapView.layerManager.layers.add(marker)
        }
    }
}