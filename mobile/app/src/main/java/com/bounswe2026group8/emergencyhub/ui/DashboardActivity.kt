package com.bounswe2026group8.emergencyhub.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.FcmTokenRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.UserData
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.map.cache.MapTileCacheHelper
import com.bounswe2026group8.emergencyhub.map.data.GatheringPointCache
import com.bounswe2026group8.emergencyhub.map.data.PreferencesManager
import com.bounswe2026group8.emergencyhub.map.ui.OfflineFeaturesActivity
import com.bounswe2026group8.emergencyhub.util.LocaleManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DashboardActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var hubSelectorHelper: HubSelectorHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tokenManager = TokenManager(this)

        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        tokenManager.getUser()?.let { displayUser(it) }
        fetchMe()
        sendFcmTokenToBackend()

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { performLogout() }

        hubSelectorHelper = HubSelectorHelper(this, findViewById<Spinner>(R.id.spinnerHubSelector))
        setupLanguageSelector()
        setupFeatureCards()
    }

    private fun setupLanguageSelector() {
        val languageSpinner = findViewById<Spinner>(R.id.spinnerLanguage)

        val languages = LocaleManager.getSupportedLanguages()
        val displayNames = languages.map { LocaleManager.getDisplayName(it) }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        val currentLanguage = LocaleManager.getLanguage(this)
        val position = languages.indexOf(currentLanguage)
        if (position >= 0) {
            languageSpinner.setSelection(position)
        }

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = languages[position]
                val currentLanguage = LocaleManager.getLanguage(this@DashboardActivity)
                if (selectedLanguage != currentLanguage) {
                    LocaleManager.setLanguage(this@DashboardActivity, selectedLanguage)
                    LocaleManager.applyLocale(this@DashboardActivity, selectedLanguage)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }
        hubSelectorHelper.load()
        preCacheOfflineData()
        // Push any locally-stored mesh messages up to the server. Fires on fresh
        // login (user just landed on dashboard) and every time the user returns
        // to dashboard — so any unsynced offline-mesh posts/comments get uploaded
        // without the user having to open the archive screen explicitly.
        lifecycleScope.launch {
            com.bounswe2026group8.emergencyhub.mesh.MeshServerSyncManager
                .uploadIfOnline(this@DashboardActivity)
        }
    }

    private fun fetchMe() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@DashboardActivity).getMe()
                if (response.isSuccessful) {
                    val user = response.body()!!
                    tokenManager.saveUser(user)
                    displayUser(user)
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLandingIfVisible()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun displayUser(user: UserData) {
        findViewById<TextView>(R.id.txtWelcome).text =
            getString(R.string.dashboard_welcome_format, user.fullName)

        val roleLabel = if (user.role == "EXPERT") {
            getString(R.string.dashboard_role_expert)
        } else {
            getString(R.string.dashboard_role_standard)
        }
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
            txtNeighborhood.text =
                getString(R.string.dashboard_neighborhood_format, user.neighborhoodAddress)
            txtNeighborhood.visibility = TextView.VISIBLE
        } else {
            txtNeighborhood.visibility = TextView.GONE
        }

        val cardStaffTools = findViewById<MaterialCardView>(R.id.cardStaffTools)
        cardStaffTools.visibility =
            if (StaffRoleHelper.hasAnyStaffRole(user.staffRole)) View.VISIBLE else View.GONE
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                RetrofitClient.getService(this@DashboardActivity).logout()
            } catch (_: Exception) {
            }
            tokenManager.clear()
            navigateToLanding()
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

        findViewById<MaterialCardView>(R.id.cardOfflineMessages).setOnClickListener {
            startActivity(
                Intent(this, com.bounswe2026group8.emergencyhub.mesh.ui.MeshArchiveActivity::class.java)
            )
        }

        findViewById<MaterialCardView>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            }
        findViewById<MaterialCardView>(R.id.cardStaffTools).setOnClickListener {
            startActivity(Intent(this, StaffDashboardActivity::class.java))
        }
    }

    private fun sendFcmTokenToBackend() {
        lifecycleScope.launch {
            try {
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                RetrofitClient.getService(this@DashboardActivity)
                    .updateFcmToken(FcmTokenRequest(fcmToken))
            } catch (_: Exception) {
            }
        }
    }

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
                    runOfflineCacheFromSaved()
                }
        } else {
            runOfflineCacheFromSaved()
        }
    }

    private fun runOfflineCacheFromSaved() {
        val (lat, lon) = PreferencesManager(this).loadUserLocation()
        runOfflineCache(lat, lon)
    }

    private fun runOfflineCache(lat: Double, lon: Double) {
        Log.d("Dashboard", "Pre-caching offline data for ($lat, $lon)")

        org.osmdroid.config.Configuration.getInstance().load(
            this, getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
        org.osmdroid.config.Configuration.getInstance().osmdroidTileCache = cacheDir
        org.osmdroid.config.Configuration.getInstance().tileFileSystemCacheMaxBytes = 50L * 1024 * 1024
        org.osmdroid.config.Configuration.getInstance().tileFileSystemCacheTrimBytes = 40L * 1024 * 1024

        MapTileCacheHelper.cacheUserArea(this, lat, lon)

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

    private fun navigateToLandingIfVisible() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && !isFinishing) {
            navigateToLanding()
        }
    }
}
