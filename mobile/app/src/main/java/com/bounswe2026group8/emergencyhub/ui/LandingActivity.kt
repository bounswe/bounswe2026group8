package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.map.ui.OfflineFeaturesActivity
import com.bounswe2026group8.emergencyhub.mesh.ui.MeshActivity
import com.google.android.material.button.MaterialButton

/**
 * Landing / Welcome screen.
 *
 * Shows the app name, a short description of the emergency preparedness
 * hub, and two buttons to navigate to Sign Up or Sign In.
 *
 * If the user already has a saved token, skips straight to the Dashboard.
 */
class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already logged in, go directly to Dashboard
        val tokenManager = TokenManager(this)
        if (tokenManager.isLoggedIn()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_landing)

        findViewById<MaterialButton>(R.id.btnSignUp).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnSignIn).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnOfflineInfo).setOnClickListener {
            startActivity(Intent(this, OfflineFeaturesActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnMeshMessaging).setOnClickListener {
            startActivity(Intent(this, MeshActivity::class.java))
        }
    }
}
