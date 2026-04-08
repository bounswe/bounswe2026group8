package com.bounswe2026group8.emergencyhub.map.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.map.cache.MapTileCacheHelper
import com.bounswe2026group8.emergencyhub.map.data.GatheringPointCache
import com.bounswe2026group8.emergencyhub.map.data.MapRepository
import com.bounswe2026group8.emergencyhub.map.data.PreferencesManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var infoText: TextView
    private lateinit var loadingContainer: LinearLayout
    private lateinit var loadingText: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) fetchAndShowCurrentLocation() else useDefaultLocation()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidTileCache = cacheDir
        // Cap tile cache at 50 MB, trim to 40 MB when exceeded
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 50L * 1024 * 1024
        Configuration.getInstance().tileFileSystemCacheTrimBytes = 40L * 1024 * 1024

        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapView = findViewById(R.id.mapView)
        infoText = findViewById(R.id.infoText)
        loadingContainer = findViewById(R.id.loadingContainer)
        loadingText = findViewById(R.id.loadingText)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        showLoading("Detecting your location…")
        checkLocationPermissionAndFetch()
    }

    private fun showLoading(message: String) {
        loadingText.text = message
        loadingContainer.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingContainer.visibility = View.GONE
    }

    private fun checkLocationPermissionAndFetch() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchAndShowCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchAndShowCurrentLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            useDefaultLocation()
            return
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient.getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    PreferencesManager(this).saveUserLocation(lat, lon)
                    showMap(lat, lon)
                    MapTileCacheHelper.cacheUserArea(this, lat, lon)
                } else {
                    useDefaultLocation()
                }
            }
            .addOnFailureListener {
                useDefaultLocation()
            }
    }

    private fun useDefaultLocation() {
        val (lat, lon) = PreferencesManager(this).loadUserLocation()
        showMap(lat, lon)
    }

    private fun showMap(lat: Double, lon: Double) {
        hideLoading()

        val center = GeoPoint(lat, lon)
        mapView.controller.setCenter(center)
        mapView.controller.setZoom(15.0)

        // User location marker — added immediately
        val userMarker = Marker(mapView)
        userMarker.position = center
        userMarker.title = "Your Location"
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userMarker.icon = scaledMarkerIcon(R.drawable.user_marker, 40)
        mapView.overlays.add(userMarker)

        infoText.text = getString(R.string.nearest_format, "Loading nearby points…")

        // Fetch gathering points in background (Overpass API or local cache)
        val gatheringPointCache = GatheringPointCache(this)
        val mapRepository = MapRepository()

        lifecycleScope.launch {
            val points = gatheringPointCache.getPoints(lat, lon)
            android.util.Log.d("MapActivity", "Got ${points.size} points for ($lat, $lon)")
            val nearestPoint = mapRepository.findNearestPoint(lat, lon, points)

            infoText.text = if (nearestPoint != null) {
                val distKm = mapRepository.distanceKm(lat, lon, nearestPoint.lat, nearestPoint.lon)
                getString(
                    R.string.nearest_format,
                    "${nearestPoint.name} (${String.format(Locale.US, "%.2f", distKm)} km)"
                )
            } else {
                getString(R.string.nearest_format, "No nearby point (${points.size} fetched)")
            }

            val normalIcon = scaledMarkerIcon(R.drawable.marker, 32)
            val nearestIcon = scaledMarkerIcon(R.drawable.marker, 42)

            for (point in points) {
                val marker = Marker(mapView)
                marker.position = GeoPoint(point.lat, point.lon)
                marker.title = point.name
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.icon = if (point == nearestPoint) nearestIcon else normalIcon
                mapView.overlays.add(marker)
            }

            mapView.invalidate()
        }
    }

    private fun scaledMarkerIcon(resId: Int, sizeDp: Int): Drawable {
        val px = (sizeDp * resources.displayMetrics.density).toInt()
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        val scaled = Bitmap.createScaledBitmap(bitmap, px, px, true)
        return BitmapDrawable(resources, scaled)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}
