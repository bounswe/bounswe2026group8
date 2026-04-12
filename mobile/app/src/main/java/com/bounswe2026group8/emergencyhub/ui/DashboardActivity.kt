package com.bounswe2026group8.emergencyhub.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.UserData
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.map.cache.MapTileCacheHelper
import com.bounswe2026group8.emergencyhub.map.data.GatheringPointCache
import com.bounswe2026group8.emergencyhub.map.data.PreferencesManager
import com.bounswe2026group8.emergencyhub.viewmodel.DashboardViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.bounswe2026group8.emergencyhub.map.ui.OfflineFeaturesActivity
import kotlinx.coroutines.launch

/**
 * Dashboard screen — the post-login home page.
 *
 * Displays:
 *   - Welcome message with user name
 *   - Role badge (STANDARD / EXPERT)
 *   - Expertise badge (if EXPERT)
 *   - Neighborhood badge (if provided)
 *   - Feature cards (Forum, Help Requests, Profile, Offline Info)
 *   - Logout button
 */
class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var hubSelectorHelper: HubSelectorHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // If no token, redirect to Landing
        if (!viewModel.isLoggedIn()) {
            navigateToLanding()
            return
        }

        // Observe user data from ViewModel
        viewModel.user.observe(this) { user -> user?.let { displayUser(it) } }
        viewModel.navigateToLanding.observe(this) { if (it) navigateToLanding() }

        viewModel.loadUser()
        viewModel.sendFcmToken()

        // Logout
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { viewModel.logout() }

        // Hub selector (load() is called in onResume)
        hubSelectorHelper = HubSelectorHelper(this, findViewById<Spinner>(R.id.spinnerHubSelector))

        // Feature cards
        setupFeatureCards()
    }

    override fun onResume() {
        super.onResume()
        hubSelectorHelper.load()
        preCacheOfflineData()
    }

    private fun displayUser(user: UserData) {
        findViewById<TextView>(R.id.txtWelcome).text = "Welcome, ${user.fullName}!"

        val roleLabel = if (user.role == "EXPERT") "🎓 Expert" else "👤 Standard"
        findViewById<TextView>(R.id.txtRole).text = roleLabel

        val txtExpertise = findViewById<TextView>(R.id.txtExpertise)
        if (!user.expertiseField.isNullOrBlank()) {
            txtExpertise.text = user.expertiseField
            txtExpertise.visibility = TextView.VISIBLE
        } else {
            txtExpertise.visibility = TextView.GONE
        }

        val txtNeighborhood = findViewById<TextView>(R.id.txtNeighborhood)
        if (!user.neighborhoodAddress.isNullOrBlank()) {
            txtNeighborhood.text = "📍 ${user.neighborhoodAddress}"
            txtNeighborhood.visibility = TextView.VISIBLE
        } else {
            txtNeighborhood.visibility = TextView.GONE
        }
    }

    private fun setupFeatureCards() {
        findViewById<MaterialCardView>(R.id.cardForum).setOnClickListener {
            startActivity(Intent(this, ForumActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardHelpRequests).setOnClickListener {
            startActivity(Intent(this, HelpRequestListActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardOfflineInfo).setOnClickListener {
            startActivity(Intent(this, OfflineFeaturesActivity::class.java))
        }
    }

    /**
     * Silently pre-caches map tiles and gathering points in the background.
     * Uses current GPS if location permission is already granted, otherwise
     * falls back to the last saved location. Never prompts for permission.
     */
    @Suppress("MissingPermission")
    private fun preCacheOfflineData() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val client = LocationServices.getFusedLocationProviderClient(this)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    val lat = location?.latitude ?: return@addOnSuccessListener
                    val lon = location.longitude
                    PreferencesManager(this).saveUserLocation(lat, lon)
                    runOfflineCache(lat, lon)
                }
                .addOnFailureListener {
                    // GPS failed — use last saved location
                    runOfflineCacheFromSaved()
                }
        } else {
            // No permission — use last saved location (default: Istanbul)
            runOfflineCacheFromSaved()
        }
    }

    private fun runOfflineCacheFromSaved() {
        val (lat, lon) = PreferencesManager(this).loadUserLocation()
        runOfflineCache(lat, lon)
    }

    private fun runOfflineCache(lat: Double, lon: Double) {
        Log.d("Dashboard", "Pre-caching offline data for ($lat, $lon)")

        // Initialize OSMDroid config so tile downloads have proper User-Agent and cache path
        org.osmdroid.config.Configuration.getInstance().load(
            this, getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
        org.osmdroid.config.Configuration.getInstance().osmdroidTileCache = cacheDir
        org.osmdroid.config.Configuration.getInstance().tileFileSystemCacheMaxBytes = 50L * 1024 * 1024
        org.osmdroid.config.Configuration.getInstance().tileFileSystemCacheTrimBytes = 40L * 1024 * 1024

        // Cache map tiles (runs on OSMDroid's internal thread pool)
        MapTileCacheHelper.cacheUserArea(this, lat, lon)

        // Cache gathering points from Overpass API
        lifecycleScope.launch {
            try {
                val cache = GatheringPointCache(this@DashboardActivity)
                val points = cache.getPoints(lat, lon)
                Log.d("Dashboard", "Pre-cached ${points.size} gathering points for ($lat, $lon)")
            } catch (e: Exception) {
                Log.e("Dashboard", "Failed to pre-cache gathering points: ${e.message}")
            }
        }
    }

    private fun navigateToLanding() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
