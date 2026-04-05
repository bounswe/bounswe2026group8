package com.bounswe2026group8.emergencyhub.map.ui

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

class MapRenderer(private val context: Context) {

    fun loadMap(mapView: MapView, mapFile: File) {
        if (!mapFile.exists() || mapFile.length() == 0L) {
            throw IllegalArgumentException("Map file missing or empty: ${mapFile.absolutePath}")
        }

        val tileCache = AndroidUtil.createTileCache(
            context,
            "mapcache",
            mapView.model.displayModel.tileSize,
            1f,
            mapView.model.frameBufferModel.overdrawFactor
        )

        val mapDataStore = MapFile(mapFile)

        val tileRendererLayer = TileRendererLayer(
            tileCache,
            mapDataStore,
            mapView.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE
        ).apply {
            setXmlRenderTheme(InternalRenderTheme.DEFAULT)
        }

        mapView.layerManager.layers.add(tileRendererLayer)
    }

    fun addMarkers(
        mapView: MapView,
        gatheringPoints: List<GatheringPoint>,
        userLocation: LatLong,
        nearestPoint: GatheringPoint?
    ) {
        val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.marker)
        val userBitmapOriginal = BitmapFactory.decodeResource(context.resources, R.drawable.user_marker)

        val userBitmap = android.graphics.Bitmap.createScaledBitmap(
            userBitmapOriginal,
            80, 80,
            true
        )
        // ✅ User marker
        val userMarker = Marker(
            userLocation,
            org.mapsforge.map.android.graphics.AndroidBitmap(userBitmap),
            0,
            -userBitmap.height / 2
        )
        mapView.layerManager.layers.add(userMarker)

        // ✅ Gathering points
        for (point in gatheringPoints) {
            val isNearest = point == nearestPoint
            val size = if (isNearest) 120 else 80

            val bitmap = android.graphics.Bitmap.createScaledBitmap(
                originalBitmap,
                size, size,
                true
            )

            val marker = object : Marker(
                LatLong(point.lat, point.lon),
                org.mapsforge.map.android.graphics.AndroidBitmap(bitmap),
                0,
                -bitmap.height / 2
            ) {
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

