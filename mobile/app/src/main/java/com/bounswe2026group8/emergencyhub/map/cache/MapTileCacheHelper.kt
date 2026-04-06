package com.bounswe2026group8.emergencyhub.map.cache

import android.content.Context
import android.util.Log
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView

/**
 * Pre-caches OSMDroid map tiles for a 5km radius around the user's location.
 *
 * Tiles are stored in OSMDroid's SQLite cache so the map works offline.
 * Call this once after obtaining the user's location while online.
 * Must be called from the main thread.
 */
object MapTileCacheHelper {

    private const val TAG = "MapTileCacheHelper"
    private const val RADIUS_KM = 3.0
    private const val MIN_ZOOM = 13
    private const val MAX_ZOOM = 16

    /**
     * Custom tile source with the same name as MAPNIK ("Mapnik") so it shares
     * OSMDroid's tile cache, but without FLAG_NO_BULK so CacheManager can use it.
     *
     * TileSourceFactory.MAPNIK has FLAG_NO_BULK set, which causes a
     * TileSourcePolicyException when downloadAreaAsyncNoUI is called.
     */
    private val BULK_TILE_SOURCE = object : OnlineTileSourceBase(
        "Mapnik",
        0, 19, 256, ".png",
        arrayOf(
            "https://a.tile.openstreetmap.org/",
            "https://b.tile.openstreetmap.org/",
            "https://c.tile.openstreetmap.org/"
        ),
        "© OpenStreetMap contributors",
        TileSourcePolicy(
            4,
            TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
            // FLAG_NO_BULK intentionally omitted — allows pre-caching
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl +
                    MapTileIndex.getZoom(pMapTileIndex) + "/" +
                    MapTileIndex.getX(pMapTileIndex) + "/" +
                    MapTileIndex.getY(pMapTileIndex) +
                    mImageFilenameEnding
        }
    }

    /**
     * Starts async tile download for a 5km bounding box around (lat, lon).
     * Returns immediately; download runs on OSMDroid's internal thread pool.
     * Must be called from the main thread.
     */
    fun cacheUserArea(context: Context, lat: Double, lon: Double) {
        try {
            Configuration.getInstance().osmdroidTileCache = context.cacheDir

            val bbox = buildBoundingBox(lat, lon, RADIUS_KM)

            val mapView = MapView(context)
            mapView.setTileSource(BULK_TILE_SOURCE)

            val cacheManager = CacheManager(mapView)
            val estimated = cacheManager.possibleTilesInArea(bbox, MIN_ZOOM, MAX_ZOOM)
            Log.d(TAG, "Starting background tile cache: ~$estimated tiles around ($lat, $lon)")

            cacheManager.downloadAreaAsyncNoUI(
                context,
                bbox,
                MIN_ZOOM,
                MAX_ZOOM,
                object : CacheManager.CacheManagerCallback {
                    override fun onTaskComplete() {
                        Log.d(TAG, "Tile caching complete")
                    }
                    override fun onTaskFailed(errors: Int) {
                        Log.w(TAG, "Tile caching finished with $errors errors")
                    }
                    override fun updateProgress(
                        progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int
                    ) {}
                    override fun downloadStarted() {}
                    override fun setPossibleTilesInArea(total: Int) {}
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tile caching: ${e.message}")
        }
    }

    /**
     * Converts a center point + radius in km into a BoundingBox.
     */
    private fun buildBoundingBox(lat: Double, lon: Double, radiusKm: Double): BoundingBox {
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)))
        return BoundingBox(
            lat + latDelta,
            lon + lonDelta,
            lat - latDelta,
            lon - lonDelta
        )
    }
}
