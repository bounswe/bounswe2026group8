package com.bounswe2026group8.emergencyhub.map.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import java.io.File
import android.graphics.BitmapFactory
import org.mapsforge.map.layer.overlay.Marker
import com.google.gson.Gson
import android.widget.Button
import com.bounswe2026group8.emergencyhub.R


class MapActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var mapRepository: MapRepository
    private lateinit var mapRenderer: MapRenderer
    private lateinit var mapScreenController: MapScreenController
    private val defaultRegion = "istanbul"

    // Fetches the user region
    fun getUserRegion(): String {
        return preferencesManager.loadUserRegion(defaultRegion)
    }

    // Fetches user Location
    fun getUserLocation(): LatLong {
        return preferencesManager.loadSimulatedLocation()
    }

    fun updateRegionFromLocation() {
        val userLocation = getUserLocation()
        val detectedRegion = mapRepository.getRegionFromLocation(userLocation)
        preferencesManager.saveUserRegion(detectedRegion)
    }

    @SuppressLint("UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidGraphicFactory.createInstance(application)
        setContentView(R.layout.activity_map)

        preferencesManager = PreferencesManager(this)
        mapRepository = MapRepository(this)
        mapRenderer = MapRenderer(this)
        mapScreenController = MapScreenController(
            this,
            preferencesManager,
            mapRepository,
            mapRenderer
        )

        val infoText = findViewById<android.widget.TextView>(R.id.infoText)

        val mapView = findViewById<MapView>(R.id.mapView)

        val changeRegionButton = findViewById<Button>(R.id.changeRegionButton)

        changeRegionButton.setOnClickListener {

            changeRegionButton.isEnabled = false

            val currentLocation = getUserLocation()
            val isCurrentlyIstanbul = currentLocation.latitude > 40.5

            if (isCurrentlyIstanbul) {
                preferencesManager.saveSimulatedLocation(39.9334, 32.8597)
            } else {
                preferencesManager.saveSimulatedLocation(41.0105, 28.985)
            }

            updateRegionFromLocation()
            mapView.setCenter(getUserLocation())
            mapScreenController.updateMap(mapView, infoText, defaultRegion)

            changeRegionButton.postDelayed({
                changeRegionButton.isEnabled = true
            }, 500)
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        updateRegionFromLocation()

        mapView.isClickable = true
        mapView.setBuiltInZoomControls(true)
        mapView.setZoomLevel(12.toByte())
        mapView.setCenter(getUserLocation()) // Istanbul

        mapScreenController.updateMap(mapView, infoText, defaultRegion)
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidGraphicFactory.clearResourceMemoryCache()
    }
}