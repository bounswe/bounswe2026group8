package com.bounswe2026group8.emergencyhub.map.ui

import android.Manifest
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bounswe2026group8.emergencyhub.R
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.File
import java.util.Locale

class MapActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var mapRepository: MapRepository
    private lateinit var mapRenderer: MapRenderer
    private lateinit var mapScreenController: MapScreenController
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private lateinit var infoText: TextView

    private val defaultRegion = "europe"
    private var activeDownloadId: Long = -1L

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {
                fetchAndShowCurrentLocation()
            } else {
                handleLocationDenied()
            }
        }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
            if (completedId == activeDownloadId) {
                Toast.makeText(
                    this@MapActivity,
                    "Map download completed.",
                    Toast.LENGTH_LONG
                ).show()
                renderMap()
            }
        }
    }

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        infoText = findViewById(R.id.infoText)
        mapView = findViewById(R.id.mapView)

        mapView.isClickable = true
        mapView.setBuiltInZoomControls(true)
        mapView.setZoomLevel(12.toByte())

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        ContextCompat.registerReceiver(
            this,
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )

        renderMap()
        checkLocationPermissionAndFetch()
    }

    private fun checkLocationPermissionAndFetch() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
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
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            handleLocationDenied()
            return
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient
            .getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLong(location.latitude, location.longitude)
                    preferencesManager.saveUserLocation(userLocation.latitude, userLocation.longitude)

                    mapView.setCenter(userLocation)
                    resolveCountryAndEnsureMap(userLocation)
                } else {
                    loadLastSavedLocation()
                }
            }
            .addOnFailureListener {
                loadLastSavedLocation()
            }

    }

    private fun handleLocationDenied() {
        Toast.makeText(
            this,
            "Location permission denied. Using saved/default location.",
            Toast.LENGTH_SHORT
        ).show()

        updateRegionFromSavedLocation()
        renderMap()
    }

    private fun loadLastSavedLocation() {
        val savedLocation = preferencesManager.loadUserLocation()

        mapView.setCenter(savedLocation)

        Toast.makeText(
            this,
            "Could not get current location. Using saved/default location.",
            Toast.LENGTH_SHORT
        ).show()

        resolveCountryAndEnsureMap(savedLocation)
    }

    private fun updateRegionFromSavedLocation() {
        val userLocation = preferencesManager.loadUserLocation()
        mapView.setCenter(userLocation)
    }

    private fun renderMap() {
        mapScreenController.updateMap(mapView, infoText, defaultRegion)
    }

    private fun resolveCountryAndEnsureMap(userLocation: LatLong) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(
                this,
                "Geocoder not available. Rendering with existing files only.",
                Toast.LENGTH_LONG
            ).show()
            renderMap()
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(
                userLocation.latitude,
                userLocation.longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val address = addresses.firstOrNull()

                        val countryName = address?.countryName ?: "Turkey"
                        val detectedRegion = if (mapRepository.isUnitedStates(countryName)) {
                            "north-america"
                        } else {
                            mapRepository.getRegionForCountry(countryName)
                        }

                        preferencesManager.saveUserCountry(countryName)
                        preferencesManager.saveUserRegion(detectedRegion)

                        if (mapRepository.isUnitedStates(countryName)) {
                            val stateName = address?.adminArea

                            if (stateName.isNullOrBlank()) {
                                Toast.makeText(
                                    this@MapActivity,
                                    "Could not resolve US state.",
                                    Toast.LENGTH_LONG
                                ).show()
                                renderMap()
                                return
                            }

                            val fileName = mapRepository.getUsStateMapFileName(stateName)
                            preferencesManager.saveMapFileName(fileName)
                            ensureUsStateMapDownloaded(stateName)
                        } else {
                            val fileName = mapRepository.getCountryMapFileName(countryName)
                            preferencesManager.saveMapFileName(fileName)
                            ensureCountryMapDownloaded(countryName)
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        Toast.makeText(
                            this@MapActivity,
                            "Could not resolve country. Using Turkey.",
                            Toast.LENGTH_SHORT
                        ).show()

                        val fallbackCountry = "Turkey"
                        val fallbackRegion = mapRepository.getRegionForCountry(fallbackCountry)
                        val fallbackFileName = mapRepository.getCountryMapFileName(fallbackCountry)

                        preferencesManager.saveUserCountry(fallbackCountry)
                        preferencesManager.saveUserRegion(fallbackRegion)
                        preferencesManager.saveMapFileName(fallbackFileName)

                        ensureCountryMapDownloaded(fallbackCountry)
                    }
                }
            )
        } else {
            @Suppress("DEPRECATION")
            try {
                val addresses = geocoder.getFromLocation(
                    userLocation.latitude,
                    userLocation.longitude,
                    1
                )

                val address = addresses?.firstOrNull()
                val countryName = address?.countryName ?: "Turkey"

                val detectedRegion = if (mapRepository.isUnitedStates(countryName)) {
                    "north-america"
                } else {
                    mapRepository.getRegionForCountry(countryName)
                }

                preferencesManager.saveUserCountry(countryName)
                preferencesManager.saveUserRegion(detectedRegion)

                if (mapRepository.isUnitedStates(countryName)) {
                    val stateName = address?.adminArea

                    if (stateName.isNullOrBlank()) {
                        Toast.makeText(
                            this,
                            "Could not resolve US state.",
                            Toast.LENGTH_LONG
                        ).show()
                        renderMap()
                        return
                    }

                    val fileName = mapRepository.getUsStateMapFileName(stateName)
                    preferencesManager.saveMapFileName(fileName)
                    ensureUsStateMapDownloaded(stateName)
                } else {
                    val fileName = mapRepository.getCountryMapFileName(countryName)
                    preferencesManager.saveMapFileName(fileName)
                    ensureCountryMapDownloaded(countryName)
                }
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "Could not resolve country. Using Turkey.",
                    Toast.LENGTH_SHORT
                ).show()

                val fallbackCountry = "Turkey"
                val fallbackRegion = mapRepository.getRegionForCountry(fallbackCountry)
                val fallbackFileName = mapRepository.getCountryMapFileName(fallbackCountry)

                preferencesManager.saveUserCountry(fallbackCountry)
                preferencesManager.saveUserRegion(fallbackRegion)
                preferencesManager.saveMapFileName(fallbackFileName)

                ensureCountryMapDownloaded(fallbackCountry)
            }
        }
    }

    private fun ensureUsStateMapDownloaded(stateName: String) {
        val mapFileName = mapRepository.getUsStateMapFileName(stateName)
        val mapFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), mapFileName)

        if (mapFile.exists()) {
            Toast.makeText(
                this,
                "$mapFileName already exists.",
                Toast.LENGTH_SHORT
            ).show()
            renderMap()
        } else {
            startUsStateMapDownload(stateName)
        }
    }

    @SuppressLint("UseKtx")
    private fun startUsStateMapDownload(stateName: String) {
        val url = mapRepository.buildUsStateMapUrl(stateName)
        val fileName = mapRepository.getUsStateMapFileName(stateName)

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading $fileName")
            .setDescription("Offline map download in progress")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                this,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        activeDownloadId = downloadManager.enqueue(request)

        Toast.makeText(
            this,
            "Started download: $fileName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun ensureCountryMapDownloaded(countryName: String) {
        val mapFileName = mapRepository.getCountryMapFileName(countryName)
        val mapFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), mapFileName)

        if (mapFile.exists()) {
            Toast.makeText(
                this,
                "$mapFileName already exists.",
                Toast.LENGTH_SHORT
            ).show()
            renderMap()
        } else {
            startCountryMapDownload(countryName)
        }
    }

    @SuppressLint("UseKtx")
    private fun startCountryMapDownload(countryName: String) {
        val fileName = mapRepository.getCountryMapFileName(countryName)

        if (!mapRepository.countryExistsInWorldJson(countryName)) {
            Toast.makeText(
                this,
                "Country not found in world.json: $countryName",
                Toast.LENGTH_LONG
            ).show()
            renderMap()
            return
        }

        val url = mapRepository.buildCountryMapUrl(countryName)

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading $fileName")
            .setDescription("Offline map download in progress")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                this,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        activeDownloadId = downloadManager.enqueue(request)

        Toast.makeText(
            this,
            "Started download: $fileName",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(downloadCompleteReceiver)
        } catch (_: Exception) {
        }

        mapView.destroyAll()
        AndroidGraphicFactory.clearResourceMemoryCache()

        super.onDestroy()
    }
}