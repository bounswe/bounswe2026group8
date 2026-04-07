package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.FcmTokenRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.UserData
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private lateinit var tokenManager: TokenManager
    private lateinit var hubSelectorHelper: HubSelectorHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tokenManager = TokenManager(this)

        // If no token, redirect to Landing
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        // Try cached user first, then refresh from /me
        tokenManager.getUser()?.let { displayUser(it) }
        fetchMe()

        // Register FCM token for push notifications
        sendFcmTokenToBackend()

        // Logout
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { performLogout() }

        // Hub selector (load() is called in onResume)
        hubSelectorHelper = HubSelectorHelper(this, findViewById<Spinner>(R.id.spinnerHubSelector))

        // Feature cards
        setupFeatureCards()
    }

    override fun onResume() {
        super.onResume()
        hubSelectorHelper.load()
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
                    // Token expired / invalid
                    tokenManager.clear()
                    navigateToLanding()
                }
            } catch (e: Exception) {
                // Network error — use cached data if available
            }
        }
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

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                RetrofitClient.getService(this@DashboardActivity).logout()
            } catch (_: Exception) {
                // Even if the server call fails, clear local state
            }
            tokenManager.clear()
            navigateToLanding()
        }
    }

    private fun setupFeatureCards() {
        // Forum — navigate to forum screen
        findViewById<MaterialCardView>(R.id.cardForum).setOnClickListener {
            startActivity(Intent(this, ForumActivity::class.java))
        }

        // Help Requests — navigate to the help center (tabbed list)
        findViewById<MaterialCardView>(R.id.cardHelpRequests).setOnClickListener {
            startActivity(Intent(this, HelpRequestListActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val placeholders = mapOf(
            R.id.cardOfflineInfo to "Offline Info"
        )
        for ((id, name) in placeholders) {
            findViewById<MaterialCardView>(id).setOnClickListener {
                Toast.makeText(this, "$name — coming soon!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendFcmTokenToBackend() {
        lifecycleScope.launch {
            try {
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                RetrofitClient.getService(this@DashboardActivity)
                    .updateFcmToken(FcmTokenRequest(fcmToken))
            } catch (_: Exception) {
                // FCM token registration failed — will retry on next launch
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
